package com.splunk.logging;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Sanitised information about a throwable that is suitable for logging. Has been decoupled from
 * any class information in the original throwable.
 */
public class HttpEventCollectorThrowableInfo {

    private final String message;
    private final String className;
    private final HttpEventCollectorThrowableInfo cause;
    private final List<String> stackTraceElements;

    public HttpEventCollectorThrowableInfo(
            final String message,
            final String className,
            final HttpEventCollectorThrowableInfo cause,
            final List<String> stackTraceElements
    ) {
        super();
        this.message = message;
        this.className = className;
        this.cause = cause;
        this.stackTraceElements = stackTraceElements;
    }

    public String getClassName() { return className; }

    public String getMessage() { return message; }

    public HttpEventCollectorThrowableInfo getCause() { return cause; }

    public List<String> getStackTraceElements() { return stackTraceElements; }

    /**
     * Builds a throwable info that includes the chain of causation with common frames removed.
     *
     * @param throwable the throwable from which the info will be constructed.
     *
     * @return the info that contains the info about the throwable.
     */
    public static HttpEventCollectorThrowableInfo buildFromThrowable(Throwable throwable) {

        if (throwable == null) {
            return null;
        }

        final List<Throwable> throwableList = getThrowables(throwable);

        HttpEventCollectorThrowableInfo cause = null;
        for (int i = throwableList.size() - 1; i >= 0; i--) {
            final Throwable t = throwableList.get(i);

            final String message = t.getMessage();

            final String className = t.getClass().getCanonicalName();

            final List<String> stackTraceElements = getStackTraceElements(t);

            if (i > 0) {
                // Peek up the causation chain so that common stack frames can be removed
                // from the current throwable.
                final Throwable wrapper = throwableList.get(i - 1);
                final List<String> wrapperStackTraceElements = getStackTraceElements(wrapper);

                removeCommonElements(stackTraceElements, wrapperStackTraceElements);
            }

            cause = new HttpEventCollectorThrowableInfo(message, className, cause, stackTraceElements);
        }

        return cause;
    }

    /**
     * Serialises the chain of causation of the specified throwable.
     *
     * Breaks if a duplicate cause is detected (e.g. if a causal loop is found).
     *
     * @param t the throwable whose chain of causation will be serialised.
     * @return the chain of causation.
     */
    private static List<Throwable> getThrowables(final Throwable t) {
        final List<Throwable> result = new LinkedList<Throwable>();

        Throwable throwable = t;
        while (throwable != null) {
            if (result.contains(throwable)) {
                break;
            }
            result.add(throwable);
            throwable = throwable.getCause();
        }

        return result;
    }

    /**
     * Extracts the stack trace as strings from the provided Throwable.
     *
     * @param t the throwable that the stack trace should be extracted from (may be null, which will produce an empty list).
     * @return the stack trace of the throwable.
     */
    private static List<String> getStackTraceElements(final Throwable t) {
        if (t == null) {
            return Collections.emptyList();
        }

        final List<String> trace = new LinkedList<String>();

        final StackTraceElement[] stackTrace = t.getStackTrace();
        for (final StackTraceElement stackTraceElement : stackTrace) {
            trace.add(stackTraceElement.toString());
        }

        return trace;
    }

    /**
     * Removes elements from the tail of causeElements that are also on the tail of elements.
     * @param causeElements list from which common elements will be removed.
     * @param elements list that will be checked for elements common to causeElements.
     */
    private static <T> void removeCommonElements(final List<T> causeElements, final List<T> elements) {
        final int maxCommonElements = Math.min(causeElements.size(), elements.size());
        for (int i = 0; i < maxCommonElements; i++) {
            final T element = elements.get(elements.size() - i - 1);
            final T causeElement = causeElements.get(causeElements.size() - 1);

            if (element.equals(causeElement)) {
                causeElements.remove(causeElements.size() - 1);
            }
            else {
                break;
            }
        }
    }
}
