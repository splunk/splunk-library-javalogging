package com.splunk.logging;

/*
 * Copyright 2013-2014 Splunk, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"): you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * <tt>SplunkCimLogEvent</tt> encapsulates the best practice logging semantics recommended by Splunk.
 *
 * It produces events of key, value pairs, properly formatted and quoted for logging with any of Java's standard
 * logging libraries (logback, log4j, java.util.logging, etc.) and indexing by Splunk. The class has convenience
 * methods to set the fields defined in the standard Splunk Common Information Model.
 *
 * <tt>SplunkCimLogEvent</tt> adds no timestamp to its fields, leaving you free to configure whatever timestamp
 * format you prefer in your logging configuration.
 *
 * <code>
 * Logger logger = LoggerFactory.getLogger("splunk.logger");
 * SplunkCimLogEvent event = new SplunkCimLogEvent("Failed Login", "sshd:failure");
 * event.setAuthApp("jane");
 * event.setAuthUser("jane");
 * event.addField("somefieldname", "foobar");
 * logger.info(event.toString());
 * </code>
 * 
 * @see <a
 *      href="http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel">Splunk
 *      CIM</a>
 * @see <a
 *      href="http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6">Splunk
 *      Logging Best Practices</a>
 */
public class SplunkCimLogEvent {
    /**
     * Delimiters to use in formatting the event.
     */
    private static final String KVDELIM = "=";
    private static final String PAIRDELIM = " ";
    private static final char QUOTE = '"';

    private LinkedHashMap<String, Object> entries;

    /**
     * @param eventName event name
     * @param eventID event ID
     */
    public SplunkCimLogEvent(String eventName, String eventID) {
        entries = new LinkedHashMap<String, Object>();

        addField(PREFIX_NAME, eventName);
        addField(PREFIX_EVENT_ID, eventID);
    }

    /**
     * Add a key value pair. The value may be any Java object which returns a sensible
     * result from its <tt>toString</tt> method.
     *
     * For logging exceptions, consider using <tt>addThrowableWithStacktrace</tt> instead.
     *
     * @param key key
     * @param value value
     */
    public void addField(String key, Object value) {
        entries.put(key, value);
    }

    /**
     * Logs an exception with its stacktrace nicely formatted for indexing and searching by Splunk.
     *
     * @param throwable
     *            the Throwable object to add to the event
     */
    public void addThrowableWithStacktrace(Throwable throwable) {

        addThrowableWithStacktrace(throwable, Integer.MAX_VALUE);
    }

    /**
     * Logs an exception with the first <tt>stacktraceDepth</tt> elements of its stacktrace nicely
     * formatted for indexing and searching by Splunk,
     *
     *
     * @param throwable
     *            the Throwable object to add to the event
     * @param stacktraceDepth
     *            maximum number of stacktrace elements to log
     */

    public void addThrowableWithStacktrace(Throwable throwable, int stacktraceDepth) {
        addField(THROWABLE_CLASS, throwable.getClass().getCanonicalName());
        addField(THROWABLE_MESSAGE, throwable.getMessage());

        StackTraceElement[] elements = throwable.getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int depth = 0; depth < elements.length && depth < stacktraceDepth; depth++) {
            if (depth > 0)
                sb.append(",");
            sb.append(elements[depth].toString());
        }

        addField(THROWABLE_STACKTRACE_ELEMENTS, sb.toString());
    }

    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"");
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        boolean first = true;
        for (String key : entries.keySet()) {
            if (!first) {
                output.append(PAIRDELIM);
            } else {
                first = false;
            }
            String value = entries.get(key).toString();

            // Escape any " that appear in the key or value.
            key = DOUBLE_QUOTE.matcher(key).replaceAll("\\\\\"");
            value = DOUBLE_QUOTE.matcher(value).replaceAll("\\\\\"");

            output.append(QUOTE).append(key).append(KVDELIM).append(value).append(QUOTE);
        }

        return output.toString();
    }


    /**
     * Event prefix fields
     */
    private static final String PREFIX_NAME = "name";
    private static final String PREFIX_EVENT_ID = "event_id";

    /**
     * Java Throwable type fields
     */
    private static final String THROWABLE_CLASS = "throwable_class";
    private static final String THROWABLE_MESSAGE = "throwable_message";
    private static final String THROWABLE_STACKTRACE_ELEMENTS = "stacktrace_elements";

    /**
     * Splunk Common Information Model(CIM) Fields
     */

    // ------------------
    // Account management
    // ------------------

    /**
     * The domain containing the user that is affected by the account management event.
     */
    public void setAcManagementDestNtDomain(String acManagementDestNtDomain) {
        addField(AC_MANAGEMENT_DEST_NT_DOMAIN, acManagementDestNtDomain);
    }
    public static String AC_MANAGEMENT_DEST_NT_DOMAIN = "dest_nt_domain";

    /**
     * Description of the account management change performed.
     */
    public void setAcManagementSignature(String acManagementSignature) {
        addField(AC_MANAGEMENT_SIGNATURE, acManagementSignature);
    }
    public static String AC_MANAGEMENT_SIGNATURE = "signature";

    /**
     * The NT source of the destination. In the case of an account management
     * event, this is the domain that contains the user that generated the
     * event.
     */
    public void setAcManagementSrcNtDomain(String acManagementSrcNtDomain) {
        addField(AC_MANAGEMENT_SRC_NT_DOMAIN, acManagementSrcNtDomain);
    }
    public static String AC_MANAGEMENT_SRC_NT_DOMAIN = "src_nt_domain";

    // ----------------------------------
    // Authentication - Access protection
    // ----------------------------------

    /**
     * The action performed on the resource. success, failure
     */
    public void setAuthAction(String authAction) {
        addField(AUTH_ACTION, authAction);
    }
    public static String AUTH_ACTION = "action";
    /**
     * The application involved in the event (such as ssh, spunk, win:local).
     */
    public void setAuthApp(String authApp) {
        addField(AUTH_APP, authApp);
    }
    public static String AUTH_APP = "app";

    /**
     * The target involved in the authentication. If your field is named
     * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
     * to make it CIM-compliant.
     */
    public void setAuthDest(String authDest) {
        addField(AUTH_DEST, authDest);
    }
    public static String AUTH_DEST = "dest";

    /**
     * The source involved in the authentication. In the case of endpoint
     * protection authentication the src is the client. If your field is named
     * src_host, src_ip, src_ipv6, or src_nt_host you can alias it as src to
     * make it CIM-compliant.. It is required for all events dealing with
     * endpoint protection (Authentication, change analysis, malware, system
     * center, and update). Note: Do not confuse this with the event source or
     * sourcetype fields.
     */
    public void setAuthSrc(String authSrc) {
        addField(AUTH_SRC, authSrc);
    }
    public static String AUTH_SRC = "src";

    /**
     * In privilege escalation events, src_user represents the user who
     * initiated the privilege escalation.
     */
    public void setAuthSrcUser(String authSrcUser) {
        addField(AUTH_SRC_USER, authSrcUser);
    }
    public static String AUTH_SRC_USER = "src_user";

    /**
     * The name of the user involved in the event, or who initiated the event.
     * For authentication privilege escalation events this should represent the
     * user targeted by the escalation.
     */
    public void setAuthUser(String authUser) {
        addField(AUTH_USER, authUser);
    }
    public static String AUTH_USER = "user";

    // ----------------------------------
    // Change analysis - Endpoint protection
    // ----------------------------------

    /**
     * The action performed on the resource.
     */
    public void setChangeEndpointProtectionAction(
            String changeEndpointProtectionAction) {
        addField(CHANGE_ENDPOINT_PROTECTION_ACTION,
                changeEndpointProtectionAction);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_ACTION = "action";

    /**
     * The type of change discovered in the change analysis event.
     */
    public void setChangeEndpointProtectionChangeType(
            String changeEndpointProtectionChangeType) {
        addField(CHANGE_ENDPOINT_PROTECTION_CHANGE_TYPE,
                changeEndpointProtectionChangeType);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_CHANGE_TYPE = "change_type";

    /**
     * The host that was affected by the change. If your field is named
     * dest_host,dest_ip,dest_ipv6, or dest_nt_host you can alias it as dest to
     * make it CIM-compliant.
     */
    public void setChangeEndpointProtectionDest(
            String changeEndpointProtectionDest) {
        addField(CHANGE_ENDPOINT_PROTECTION_DEST, changeEndpointProtectionDest);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_DEST = "dest";

    /**
     * The hash signature of the modified resource.
     */
    public void setChangeEndpointProtectionHash(
            String changeEndpointProtectionHash) {
        addField(CHANGE_ENDPOINT_PROTECTION_HASH, changeEndpointProtectionHash);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_HASH = "hash";

    /**
     * The group ID of the modified resource.
     */
    public void setChangeEndpointProtectionGid(long changeEndpointProtectionGid) {
        addField(CHANGE_ENDPOINT_PROTECTION_GID, changeEndpointProtectionGid);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_GID = "gid";

    /**
     * Indicates whether or not the modified resource is a directory.
     */
    public void setChangeEndpointProtectionIsdr(
            boolean changeEndpointProtectionIsdr) {
        addField(CHANGE_ENDPOINT_PROTECTION_ISDR, changeEndpointProtectionIsdr);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_ISDR = "isdr";

    /**
     * The permissions mode of the modified resource.
     */
    public void setChangeEndpointProtectionMode(
            long changeEndpointProtectionMode) {
        addField(CHANGE_ENDPOINT_PROTECTION_MODE, changeEndpointProtectionMode);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_MODE = "mode";

    /**
     * The modification time of the modified resource.
     */
    public void setChangeEndpointProtectionModtime(
            String changeEndpointProtectionModtime) {
        addField(CHANGE_ENDPOINT_PROTECTION_MODTIME,
                changeEndpointProtectionModtime);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_MODTIME = "modtime";

    /**
     * The file path of the modified resource.
     */
    public void setChangeEndpointProtectionPath(
            String changeEndpointProtectionPath) {
        addField(CHANGE_ENDPOINT_PROTECTION_PATH, changeEndpointProtectionPath);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_PATH = "path";

    /**
     * The size of the modified resource.
     */
    public void setChangeEndpointProtectionSize(
            long changeEndpointProtectionSize) {
        addField(CHANGE_ENDPOINT_PROTECTION_SIZE, changeEndpointProtectionSize);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_SIZE = "size";

    /**
     * The user ID of the modified resource.
     */
    public void setChangeEndpointProtectionUid(long changeEndpointProtectionUid) {
        addField(CHANGE_ENDPOINT_PROTECTION_UID, changeEndpointProtectionUid);
    }
    public static String CHANGE_ENDPOINT_PROTECTION_UID = "uid";

    // ----------------------------------
    // Change analysis - Network protection
    // ----------------------------------

    /**
     * The type of change observed.
     */
    public void setChangeNetworkProtectionAction(
            String changeNetworkProtectionAction) {
        addField(CHANGE_NETWORK_PROTECTION_ACTION, changeNetworkProtectionAction);
    }
    public static String CHANGE_NETWORK_PROTECTION_ACTION = "action";

    /**
     * The command that initiated the change.
     */
    public void setChangeNetworkProtectionCommand(
            String changeNetworkProtectionCommand) {
        addField(CHANGE_NETWORK_PROTECTION_COMMAND,
                changeNetworkProtectionCommand);
    }
    public static String CHANGE_NETWORK_PROTECTION_COMMAND = "command";

    /**
     * The device that is directly affected by the change.
     */
    public void setChangeNetworkProtectionDvc(String changeNetworkProtectionDvc) {
        addField(CHANGE_NETWORK_PROTECTION_DVC, changeNetworkProtectionDvc);
    }
    public static String CHANGE_NETWORK_PROTECTION_DVC = "dvc";

    /**
     * The user that initiated the change.
     */
    public void setChangeNetworkProtectionUser(
            String changeNetworkProtectionUser) {
        addField(CHANGE_NETWORK_PROTECTION_USER, changeNetworkProtectionUser);
    }
    public static String CHANGE_NETWORK_PROTECTION_USER = "user";

    // ----------------------------------
    // Common event fields
    // ----------------------------------

    /**
     * A device-specific classification provided as part of the event.
     */
    public void setCommonCategory(String commonCategory) {
        addField(COMMON_CATEGORY, commonCategory);
    }
    public static String COMMON_CATEGORY = "category";

    /**
     * A device-specific classification provided as part of the event.
     */
    public void setCommonCount(String commonCount) {
        addField(COMMON_COUNT, commonCount);
    }
    public static String COMMON_COUNT = "count";

    /**
     * The free-form description of a particular event.
     */
    public void setCommonDesc(String commonDesc) {
        addField(COMMON_DESC, commonDesc);
    }
    public static String COMMON_DESC = "desc";

    /**
     * The name of a given DHCP pool on a DHCP server.
     */
    public void setCommonDhcpPool(String commonDhcpPool) {
        addField(COMMON_DHCP_POOL, commonDhcpPool);
    }
    public static String COMMON_DHCP_POOL = "dhcp_pool";

    /**
     * The amount of time the event lasted.
     */
    public void setCommonDuration(long commonDuration) {
        addField(COMMON_DURATION, commonDuration);
    }
    public static String COMMON_DURATION = "duration";

    /**
     * The fully qualified domain name of the device transmitting or recording
     * the log record.
     */
    public void setCommonDvcHost(String commonDvcHost) {
        addField(COMMON_DVC_HOST, commonDvcHost);
    }
    public static String COMMON_DVC_HOST = "dvc_host";

    /**
     * The IPv4 address of the device reporting the event.
     */
    public void setCommonDvcIp(String commonDvcIp) {
        addField(COMMON_DVC_IP, commonDvcIp);
    }
    public static String COMMON_DVC_IP = "dvc_ip";

    /**
     * The IPv6 address of the device reporting the event.
     */
    public void setCommonDvcIp6(String commonDvcIp6) {
        addField(COMMON_DVC_IP6, commonDvcIp6);
    }
    public static String COMMON_DVC_IP6 = "dvc_ip6";

    /**
     * The free-form description of the device's physical location.
     */
    public void setCommonDvcLocation(String commonDvcLocation) {
        addField(COMMON_DVC_LOCATION, commonDvcLocation);
    }
    public static String COMMON_DVC_LOCATION = "dvc_location";

    /**
     * The MAC (layer 2) address of the device reporting the event.
     */
    public void setCommonDvcMac(String commonDvcMac) {
        addField(COMMON_DVC_MAC, commonDvcMac);
    }
    public static String COMMON_DVC_MAC = "dvc_mac";

    /**
     * The Windows NT domain of the device recording or transmitting the event.
     */
    public void setCommonDvcNtDomain(String commonDvcNtDomain) {
        addField(COMMON_DVC_NT_DOMAIN, commonDvcNtDomain);
    }
    public static String COMMON_DVC_NT_DOMAIN = "dvc_nt_domain";

    /**
     * The Windows NT host name of the device recording or transmitting the
     * event.
     */
    public void setCommonDvcNtHost(String commonDvcNtHost) {
        addField(COMMON_DVC_NT_HOST, commonDvcNtHost);
    }
    public static String COMMON_DVC_NT_HOST = "dvc_nt_host";

    /**
     * Time at which the device recorded the event.
     */
    public void setCommonDvcTime(long commonDvcTime) {
        addField(COMMON_DVC_TIME, commonDvcTime);
    }
    public static String COMMON_DVC_TIME = "dvc_time";

    /**
     * The event's specified end time.
     */
    public void setCommonEndTime(long commonEndTime) {
        addField(COMMON_END_TIME, commonEndTime);
    }
    public static String COMMON_END_TIME = "end_time";

    /**
     * A unique identifier that identifies the event. This is unique to the
     * reporting device.
     */
    public void setCommonEventId(long commonEventId) {
        addField(COMMON_EVENT_ID, commonEventId);
    }
    public static String COMMON_EVENT_ID = "event_id";

    /**
     * The length of the datagram, event, message, or packet.
     */
    public void setCommonLength(long commonLength) {
        addField(COMMON_LENGTH, commonLength);
    }
    public static String COMMON_LENGTH = "length";

    /**
     * The log-level that was set on the device and recorded in the event.
     */
    public void setCommonLogLevel(String commonLogLevel) {
        addField(COMMON_LOG_LEVEL, commonLogLevel);
    }
    public static String COMMON_LOG_LEVEL = "log_level";

    /**
     * The name of the event as reported by the device. The name should not
     * contain information that's already being parsed into other fields from
     * the event, such as IP addresses.
     */
    public void setCommonName(String commonName) {
        addField(COMMON_NAME, commonName);
    }
    public static String COMMON_NAME = "name";

    /**
     * An integer assigned by the device operating system to the process
     * creating the record.
     */
    public void setCommonPid(long commonPid) {
        addField(COMMON_PID, commonPid);
    }
    public static String COMMON_PID = "pid";

    /**
     * An environment-specific assessment of the event's importance, based on
     * elements such as event severity, business function of the affected
     * system, or other locally defined variables.
     */
    public void setCommonPriority(long commonPriority) {
        addField(COMMON_PRIORITY, commonPriority);
    }
    public static String COMMON_PRIORITY = "priority";

    /**
     * The product that generated the event.
     */
    public void setCommonProduct(String commonProduct) {
        addField(COMMON_PRODUCT, commonProduct);
    }
    public static String COMMON_PRODUCT = "product";

    /**
     * The version of the product that generated the event.
     */
    public void setCommonProductVersion(long commonProductVersion) {
        addField(COMMON_PRODUCT_VERSION, commonProductVersion);
    }
    public static String COMMON_PRODUCT_VERSION = "product_version";

    /**
     * The result root cause, such as connection refused, timeout, crash, and so
     * on.
     */
    public void setCommonReason(String commonReason) {
        addField(COMMON_REASON, commonReason);
    }
    public static String COMMON_REASON = "reason";

    /**
     * The action result. Often is a binary choice: succeeded and failed,
     * allowed and denied, and so on.
     */
    public void setCommonResult(String commonResult) {
        addField(COMMON_RESULT, commonResult);
    }
    public static String COMMON_RESULT = "result";

    /**
     * The severity (or priority) of an event as reported by the originating
     * device.
     */
    public void setCommonSeverity(String commonSeverity) {
        addField(COMMON_SEVERITY, commonSeverity);
    }
    public static String COMMON_SEVERITY = "severity";

    /**
     * The event's specified start time.
     */
    public void setCommonStartTime(long commonStartTime) {
        addField(COMMON_START_TIME, commonStartTime);
    }
    public static String COMMON_START_TIME = "start_time";

    /**
     * The transaction identifier.
     */
    public void setCommonTransactionId(String commonTransactionId) {
        addField(COMMON_TRANSACTION_ID, commonTransactionId);
    }
    public static String COMMON_TRANSACTION_ID = "transaction_id";

    /**
     * A uniform record locator (a web address, in other words) included in a
     * record.
     */
    public void setCommonUrl(String commonUrl) {
        addField(COMMON_URL, commonUrl);
    }
    public static String COMMON_URL = "url";

    /**
     * The vendor who made the product that generated the event.
     */
    public void setCommonVendor(String commonVendor) {
        addField(COMMON_VENDOR, commonVendor);
    }
    public static String COMMON_VENDOR = "vendor";

    // ----------------------------------
    // DNS protocol
    // ----------------------------------

    /**
     * The DNS domain that has been queried.
     */
    public void setDnsDestDomain(String dnsDestDomain) {
        addField(DNS_DEST_DOMAIN, dnsDestDomain);
    }
    public static String DNS_DEST_DOMAIN = "dest_domain";

    /**
     * The remote DNS resource record being acted upon.
     */
    public void setDnsDestRecord(String dnsDestRecord) {
        addField(DNS_DEST_RECORD, dnsDestRecord);
    }
    public static String DNS_DEST_RECORD = "dest_record";

    /**
     * The DNS zone that is being received by the slave as part of a zone
     * transfer.
     */
    public void setDnsDestZone(String dnsDestZone) {
        addField(DNS_DEST_ZONE, dnsDestZone);
    }
    public static String DNS_DEST_ZONE = "dest_zone";

    /**
     * The DNS resource record class.
     */
    public void setDnsRecordClass(String dnsRecordClass) {
        addField(DNS_RECORD_CLASS, dnsRecordClass);
    }
    public static String DNS_RECORD_CLASS = "record_class";

    /**
     * The DNS resource record type.
     * 
     * @see <a
     *      href="https://secure.wikimedia.org/wikipedia/en/wiki/List_of_DNS_record_types">see
     *      this Wikipedia article on DNS record types</a>
     */
    public void setDnsRecordType(String dnsRecordType) {
        addField(DNS_RECORD_TYPE, dnsRecordType);
    }
    public static String DNS_RECORD_TYPE = "record_type";

    /**
     * The local DNS domain that is being queried.
     */
    public void setDnsSrcDomain(String dnsSrcDomain) {
        addField(DNS_SRC_DOMAIN, dnsSrcDomain);
    }
    public static String DNS_SRC_DOMAIN = "src_domain";

    /**
     * The local DNS resource record being acted upon.
     */
    public void setDnsSrcRecord(String dnsSrcRecord) {
        addField(DNS_SRC_RECORD, dnsSrcRecord);
    }
    public static String DNS_SRC_RECORD = "src_record";

    /**
     * The DNS zone that is being transferred by the master as part of a zone
     * transfer.
     */
    public void setDnsSrcZone(String dnsSrcZone) {
        addField(DNS_SRC_ZONE, dnsSrcZone);
    }
    public static String DNS_SRC_ZONE = "src_zone";

    // ----------------------------------
    // Email tracking
    // ----------------------------------

    /**
     * The person to whom an email is sent.
     */
    public void setEmailRecipient(String emailRecipient) {
        addField(EMAIL_RECIPIENT, emailRecipient);
    }
    public static String EMAIL_RECIPIENT = "recipient";

    /**
     * The person responsible for sending an email.
     */
    public void setEmailSender(String emailSender) {
        addField(EMAIL_SENDER, emailSender);
    }
    public static String EMAIL_SENDER = "sender";

    /**
     * The email subject line.
     */
    public void setEmailSubject(String emailSubject) {
        addField(EMAIL_SUBJECT, emailSubject);
    }
    public static String EMAIL_SUBJECT = "subject";

    // ----------------------------------
    // File management
    // ----------------------------------

    /**
     * The time the file (the object of the event) was accessed.
     */
    public void setFileAccessTime(long fileAccessTime) {
        addField(FILE_ACCESS_TIME, fileAccessTime);
    }
    public static String FILE_ACCESS_TIME = "file_access_time";

    /**
     * The time the file (the object of the event) was created.
     */
    public void setFileCreateTime(long fileCreateTime) {
        addField(FILE_CREATE_TIME, fileCreateTime);
    }
    public static String FILE_CREATE_TIME = "file_create_time";

    /**
     * A cryptographic identifier assigned to the file object affected by the
     * event.
     */
    public void setFileHash(String fileHash) {
        addField(FILE_HASH, fileHash);
    }
    public static String FILE_HASH = "file_hash";

    /**
     * The time the file (the object of the event) was altered.
     */
    public void setFileModifyTime(long fileModifyTime) {
        addField(FILE_MODIFY_TIME, fileModifyTime);
    }
    public static String FILE_MODIFY_TIME = "file_modify_time";

    /**
     * The name of the file that is the object of the event (without location
     * information related to local file or directory structure).
     */
    public void setFileName(String fileName) {
        addField(FILE_NAME, fileName);
    }
    public static String FILE_NAME = "file_name";

    /**
     * The location of the file that is the object of the event, in terms of
     * local file and directory structure.
     */
    public void setFilePath(String filePath) {
        addField(FILE_PATH, filePath);
    }
    public static String FILE_PATH = "file_path";

    /**
     * Access controls associated with the file affected by the event.
     */
    public void setFilePermission(String filePermission) {
        addField(FILE_PERMISSION, filePermission);
    }
    public static String FILE_PERMISSION = "file_permission";

    /**
     * The size of the file that is the object of the event. Indicate whether
     * Bytes, KB, MB, GB.
     */
    public void setFileSize(long fileSize) {
        addField(FILE_SIZE, fileSize);
    }
    public static String FILE_SIZE = "file_size";

    // ----------------------------------
    // Intrusion detection
    // ----------------------------------

    /**
     * The category of the triggered signature.
     */
    public void setIntrusionDetectionCategory(String intrusionDetectionCategory) {
        addField(INTRUSION_DETECTION_CATEGORY, intrusionDetectionCategory);
    }
    public static String INTRUSION_DETECTION_CATEGORY = "category";

    /**
     * The destination of the attack detected by the intrusion detection system
     * (IDS). If your field is named dest_host, dest_ip, dest_ipv6, or
     * dest_nt_host you can alias it as dest to make it CIM-compliant.
     */
    public void setIntrusionDetectionDest(String intrusionDetectionDest) {
        addField(INTRUSION_DETECTION_DEST, intrusionDetectionDest);
    }
    public static String INTRUSION_DETECTION_DEST = "dest";

    /**
     * The device that detected the intrusion event.
     */
    public void setIntrusionDetectionDvc(String intrusionDetectionDvc) {
        addField(INTRUSION_DETECTION_DVC, intrusionDetectionDvc);
    }
    public static String INTRUSION_DETECTION_DVC = "dvc";

    /**
     * The type of IDS that generated the event.
     */
    public void setIntrusionDetectionIdsType(String intrusionDetectionIdsType) {
        addField(INTRUSION_DETECTION_IDS_TYPE, intrusionDetectionIdsType);
    }
    public static String INTRUSION_DETECTION_IDS_TYPE = "ids_type";

    /**
     * The product name of the vendor technology generating network protection
     * data, such as IDP, Providentia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setIntrusionDetectionProduct(String intrusionDetectionProduct) {
        addField(INTRUSION_DETECTION_PRODUCT, intrusionDetectionProduct);
    }
    public static String INTRUSION_DETECTION_PRODUCT = "product";

    /**
     * The severity of the network protection event (such as critical, high,
     * medium, low, or informational).
     * 
     * Note: This field is a string. Please use a severity_id field for severity
     * ID fields that are integer data types.
     */
    public void setIntrusionDetectionSeverity(String intrusionDetectionSeverity) {
        addField(INTRUSION_DETECTION_SEVERITY, intrusionDetectionSeverity);
    }
    public static String INTRUSION_DETECTION_SEVERITY = "severity";

    /**
     * The name of the intrusion detected on the client (the src), such as
     * PlugAndPlay_BO and JavaScript_Obfuscation_Fre.
     */
    public void setIntrusionDetectionSignature(
            String intrusionDetectionSignature) {
        addField(INTRUSION_DETECTION_SIGNATURE, intrusionDetectionSignature);
    }
    public static String INTRUSION_DETECTION_SIGNATURE = "signature";

    /**
     * The source involved in the attack detected by the IDS. If your field is
     * named src_host, src_ip, src_ipv6, or src_nt_host you can alias it as src
     * to make it CIM-compliant.
     */
    public void setIntrusionDetectionSrc(String intrusionDetectionSrc) {
        addField(INTRUSION_DETECTION_SRC, intrusionDetectionSrc);
    }
    public static String INTRUSION_DETECTION_SRC = "src";

    /**
     * The user involved with the intrusion detection event.
     */
    public void setIntrusionDetectionUser(String intrusionDetectionUser) {
        addField(INTRUSION_DETECTION_USER, intrusionDetectionUser);
    }
    public static String INTRUSION_DETECTION_USER = "user";

    /**
     * The vendor technology used to generate network protection data, such as
     * IDP, Providentia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setIntrusionDetectionVendor(String intrusionDetectionVendor) {
        addField(INTRUSION_DETECTION_VENDOR, intrusionDetectionVendor);
    }
    public static String INTRUSION_DETECTION_VENDOR = "vendor";


    // ----------------------------------
    // Malware - Endpoint protection
    // ----------------------------------

    /**
     * The outcome of the infection
     */
    public void setMalwareEndpointProtectionAction(
            String malwareEndpointProtectionAction) {
        addField(MALWARE_ENDPOINT_PROTECTION_ACTION,
                malwareEndpointProtectionAction);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_ACTION = "action";

    /**
     * The NT domain of the destination (the dest_bestmatch).
     */
    public void setMalwareEndpointProtectionDestNtDomain(
            String malwareEndpointProtectionDestNtDomain) {
        addField(MALWARE_ENDPOINT_PROTECTION_DEST_NT_DOMAIN,
                malwareEndpointProtectionDestNtDomain);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_DEST_NT_DOMAIN = "dest_nt_domain";

    /**
     * The cryptographic hash of the file associated with the malware event
     * (such as the malicious or infected file).
     */
    public void setMalwareEndpointProtectionFileHash(
            String malwareEndpointProtectionFileHash) {
        addField(MALWARE_ENDPOINT_PROTECTION_FILE_HASH,
                malwareEndpointProtectionFileHash);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_FILE_HASH = "file_hash";

    /**
     * The name of the file involved in the malware event (such as the infected
     * or malicious file).
     */
    public void setMalwareEndpointProtectionFileName(
            String malwareEndpointProtectionFileName) {
        addField(MALWARE_ENDPOINT_PROTECTION_FILE_NAME,
                malwareEndpointProtectionFileName);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_FILE_NAME = "file_name";

    /**
     * The path of the file involved in the malware event (such as the infected
     * or malicious file).
     */
    public void setMalwareEndpointProtectionFilePath(
            String malwareEndpointProtectionFilePath) {
        addField(MALWARE_ENDPOINT_PROTECTION_FILE_PATH,
                malwareEndpointProtectionFilePath);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_FILE_PATH = "file_path";

    /**
     * The product name of the vendor technology (the vendor field) that is
     * generating malware data (such as Antivirus or EPO).
     */
    public void setMalwareEndpointProtectionProduct(
            String malwareEndpointProtectionProduct) {
        addField(MALWARE_ENDPOINT_PROTECTION_PRODUCT,
                malwareEndpointProtectionProduct);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_PRODUCT = "product";

    /**
     * The product version number of the vendor technology installed on the
     * client (such as 10.4.3 or 11.0.2).
     */
    public void setMalwareEndpointProtectionProductVersion(
            String malwareEndpointProtectionProductVersion) {
        addField(MALWARE_ENDPOINT_PROTECTION_PRODUCT_VERSION,
                malwareEndpointProtectionProductVersion);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_PRODUCT_VERSION = "product_version";

    /**
     * The name of the malware infection detected on the client (the src), such
     * as Trojan.Vundo,Spyware.Gaobot,W32.Nimbda).
     * 
     * Note: This field is a string. Please use a signature_id field for
     * signature ID fields that are integer data types.
     */
    public void setMalwareEndpointProtectionSignature(
            String malwareEndpointProtectionSignature) {
        addField(MALWARE_ENDPOINT_PROTECTION_SIGNATURE,
                malwareEndpointProtectionSignature);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_SIGNATURE = "signature";

    /**
     * The current signature definition set running on the client, such as
     * 11hsvx)
     */
    public void setMalwareEndpointProtectionSignatureVersion(
            String malwareEndpointProtectionSignatureVersion) {
        addField(MALWARE_ENDPOINT_PROTECTION_SIGNATURE_VERSION,
                malwareEndpointProtectionSignatureVersion);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_SIGNATURE_VERSION = "signature_version";

    /**
     * The target affected or infected by the malware. If your field is named
     * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
     * to make it CIM-compliant.
     */
    public void setMalwareEndpointProtectionDest(
            String malwareEndpointProtectionDest) {
        addField(MALWARE_ENDPOINT_PROTECTION_DEST, malwareEndpointProtectionDest);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_DEST = "dest";

    /**
     * The NT domain of the source (the src).
     */
    public void setMalwareEndpointProtectionSrcNtDomain(
            String malwareEndpointProtectionSrcNtDomain) {
        addField(MALWARE_ENDPOINT_PROTECTION_SRC_NT_DOMAIN,
                malwareEndpointProtectionSrcNtDomain);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_SRC_NT_DOMAIN = "src_nt_domain";

    /**
     * The name of the user involved in the malware event.
     */
    public void setMalwareEndpointProtectionUser(
            String malwareEndpointProtectionUser) {
        addField(MALWARE_ENDPOINT_PROTECTION_USER, malwareEndpointProtectionUser);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_USER = "user";

    /**
     * The name of the vendor technology generating malware data, such as
     * Symantec or McAfee.
     */
    public void setMalwareEndpointProtectionVendor(
            String malwareEndpointProtectionVendor) {
        addField(MALWARE_ENDPOINT_PROTECTION_VENDOR,
                malwareEndpointProtectionVendor);
    }
    public static String MALWARE_ENDPOINT_PROTECTION_VENDOR = "vendor";

    // ----------------------------------
    // Malware - Network protection
    // ----------------------------------

    /**
     * The product name of the vendor technology generating network protection
     * data, such as IDP, Proventia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setMalwareNetworkProtectionProduct(
            String malwareNetworkProtectionProduct) {
        addField(MALWARE_NETWORK_PROTECTION_PRODUCT,
                malwareNetworkProtectionProduct);
    }
    public static String MALWARE_NETWORK_PROTECTION_PRODUCT = "product";

    /**
     * The severity of the network protection event (such as critical, high,
     * medium, low, or informational).
     * 
     * Note: This field is a string. Please use a severity_id field for severity
     * ID fields that are integer data types.
     */
    public void setMalwareNetworkProtectionSeverity(
            String malwareNetworkProtectionSeverity) {
        addField(MALWARE_NETWORK_PROTECTION_SEVERITY,
                malwareNetworkProtectionSeverity);
    }
    public static String MALWARE_NETWORK_PROTECTION_SEVERITY = "severity";

    /**
     * The vendor technology used to generate network protection data, such as
     * IDP, Proventia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setMalwareNetworkProtectionVendor(
            String malwareNetworkProtectionVendor) {
        addField(MALWARE_NETWORK_PROTECTION_VENDOR,
                malwareNetworkProtectionVendor);
    }
    public static String MALWARE_NETWORK_PROTECTION_VENDOR = "vendor";


    // ----------------------------------
    // Network traffic - ESS
    // ----------------------------------

    /**
     * The action of the network traffic.
     */
    public void setNetworkTrafficEssAction(String networkTrafficEssAction) {
        addField(NETWORK_TRAFFIC_ESS_ACTION, networkTrafficEssAction);
    }
    public static String NETWORK_TRAFFIC_ESS_ACTION = "action";

    /**
     * The destination port of the network traffic.
     */
    public void setNetworkTrafficEssDestPort(int networkTrafficEssDestPort) {
        addField(NETWORK_TRAFFIC_ESS_DEST_PORT, networkTrafficEssDestPort);
    }
    public static String NETWORK_TRAFFIC_ESS_DEST_PORT = "dest_port";

    /**
     * The product name of the vendor technology generating NetworkProtection
     * data, such as IDP, Proventia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setNetworkTrafficEssProduct(String networkTrafficEssProduct) {
        addField(NETWORK_TRAFFIC_ESS_PRODUCT, networkTrafficEssProduct);
    }
    public static String NETWORK_TRAFFIC_ESS_PRODUCT = "product";

    /**
     * The source port of the network traffic.
     */
    public void setNetworkTrafficEssSrcPort(int networkTrafficEssSrcPort) {
        addField(NETWORK_TRAFFIC_ESS_SRC_PORT, networkTrafficEssSrcPort);
    }
    public static String NETWORK_TRAFFIC_ESS_SRC_PORT = "src_port";

    /**
     * The vendor technology used to generate NetworkProtection data, such as
     * IDP, Proventia, and ASA.
     * 
     * Note: Required for all events dealing with network protection (Change
     * analysis, proxy, malware, intrusion detection, packet filtering, and
     * vulnerability).
     */
    public void setNetworkTrafficEssVendor(String networkTrafficEssVendor) {
        addField(NETWORK_TRAFFIC_ESS_VENDOR, networkTrafficEssVendor);
    }
    public static String NETWORK_TRAFFIC_ESS_VENDOR = "vendor";

    // ----------------------------------
    // Network traffic - Generic
    // ----------------------------------

    /**
     * The ISO layer 7 (application layer) protocol, such as HTTP, HTTPS, SSH,
     * and IMAP.
     */
    public void setNetworkTrafficGenericAppLayer(
            String networkTrafficGenericAppLayer) {
        addField(NETWORK_TRAFFIC_GENERIC_APP_LAYER,
                networkTrafficGenericAppLayer);
    }
    public static String NETWORK_TRAFFIC_GENERIC_APP_LAYER = "app_layer";
    /**
     * How many bytes this device/interface received.
     */
    public void setNetworkTrafficGenericBytesIn(
            long networkTrafficGenericBytesIn) {
        addField(NETWORK_TRAFFIC_GENERIC_BYTES_IN, networkTrafficGenericBytesIn);
    }
    public static String NETWORK_TRAFFIC_GENERIC_BYTES_IN = "bytes_in";


    /**
     * How many bytes this device/interface transmitted.
     */
    public void setNetworkTrafficGenericBytesOut(
            long networkTrafficGenericBytesOut) {
        addField(NETWORK_TRAFFIC_GENERIC_BYTES_OUT,
                networkTrafficGenericBytesOut);
    }
    public static String NETWORK_TRAFFIC_GENERIC_BYTES_OUT = "bytes_out";

    /**
     * 802.11 channel number used by a wireless network.
     */
    public void setNetworkTrafficGenericChannel(
            String networkTrafficGenericChannel) {
        addField(NETWORK_TRAFFIC_GENERIC_CHANNEL, networkTrafficGenericChannel);
    }
    public static String NETWORK_TRAFFIC_GENERIC_CHANNEL = "channel";

    /**
     * The Common Vulnerabilities and Exposures (CVE) reference value.
     */
    public void setNetworkTrafficGenericCve(String networkTrafficGenericCve) {
        addField(NETWORK_TRAFFIC_GENERIC_CVE, networkTrafficGenericCve);
    }
    public static String NETWORK_TRAFFIC_GENERIC_CVE = "cve";

    /**
     * The destination application being targeted.
     */
    public void setNetworkTrafficGenericDestApp(
            String networkTrafficGenericDestApp) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_APP, networkTrafficGenericDestApp);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_APP = "dest_app";

    /**
     * The destination command and control service channel.
     */
    public void setNetworkTrafficGenericDestCncChannel(
            String networkTrafficGenericDestCncChannel) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_CHANNEL,
                networkTrafficGenericDestCncChannel);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_CHANNEL = "dest_cnc_channel";

    /**
     * The destination command and control service name.
     */
    public void setNetworkTrafficGenericDestCncName(
            String networkTrafficGenericDestCncName) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_NAME,
                networkTrafficGenericDestCncName);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_NAME = "dest_cnc_name";

    /**
     * The destination command and control service port.
     */
    public void setNetworkTrafficGenericDestCncPort(
            String networkTrafficGenericDestCncPort) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_PORT,
                networkTrafficGenericDestCncPort);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_PORT = "dest_cnc_port";

    /**
     * The country associated with a packet's recipient.
     */
    public void setNetworkTrafficGenericDestCountry(
            String networkTrafficGenericDestCountry) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_COUNTRY,
                networkTrafficGenericDestCountry);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_COUNTRY = "dest_country";

    /**
     * The fully qualified host name of a packet's recipient. For HTTP sessions,
     * this is the host header.
     */
    public void setNetworkTrafficGenericDestHost(
            String networkTrafficGenericDestHost) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_HOST,
                networkTrafficGenericDestHost);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_HOST = "dest_host";

    /**
     * The interface that is listening remotely or receiving packets locally.
     */
    public void setNetworkTrafficGenericDestInt(
            String networkTrafficGenericDestInt) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_INT, networkTrafficGenericDestInt);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_INT = "dest_int";

    /**
     * The IPv4 address of a packet's recipient.
     */
    public void setNetworkTrafficGenericDestIp(
            String networkTrafficGenericDestIp) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_IP, networkTrafficGenericDestIp);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_IP = "dest_ip";

    /**
     * The IPv6 address of a packet's recipient.
     */
    public void setNetworkTrafficGenericDestIpv6(
            String networkTrafficGenericDestIpv6) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_IPV6,
                networkTrafficGenericDestIpv6);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_IPV6 = "dest_ipv6";

    /**
     * The (physical) latitude of a packet's destination.
     */
    public void setNetworkTrafficGenericDestLat(int networkTrafficGenericDestLat) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_LAT, networkTrafficGenericDestLat);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_LAT = "dest_lat";

    /**
     * The (physical) longitude of a packet's destination.
     */
    public void setNetworkTrafficGenericDestLong(
            int networkTrafficGenericDestLong) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_LONG,
                networkTrafficGenericDestLong);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_LONG = "dest_long";

    /**
     * The destination TCP/IP layer 2 Media Access Control (MAC) address of a
     * packet's destination.
     */
    public void setNetworkTrafficGenericDestMac(
            String networkTrafficGenericDestMac) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_MAC, networkTrafficGenericDestMac);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_MAC = "dest_mac";

    /**
     * The Windows NT domain containing a packet's destination.
     */
    public void setNetworkTrafficGenericDestNtDomain(
            String networkTrafficGenericDestNtDomain) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_NT_DOMAIN,
                networkTrafficGenericDestNtDomain);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_NT_DOMAIN = "dest_nt_domain";

    /**
     * The Windows NT host name of a packet's destination.
     */
    public void setNetworkTrafficGenericDestNtHost(
            String networkTrafficGenericDestNtHost) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_NT_HOST,
                networkTrafficGenericDestNtHost);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_NT_HOST = "dest_nt_host";

    /**
     * TCP/IP port to which a packet is being sent.
     */
    public void setNetworkTrafficGenericDestPort(
            int networkTrafficGenericDestPort) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_PORT,
                networkTrafficGenericDestPort);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_PORT = "dest_port";

    /**
     * The NATed IPv4 address to which a packet has been sent.
     */
    public void setNetworkTrafficGenericDestTranslatedIp(
            String networkTrafficGenericDestTranslatedIp) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_IP,
                networkTrafficGenericDestTranslatedIp);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_IP = "dest_translated_ip";

    /**
     * The NATed port to which a packet has been sent.
     */
    public void setNetworkTrafficGenericDestTranslatedPort(
            int networkTrafficGenericDestTranslatedPort) {
        addField(NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_PORT,
                networkTrafficGenericDestTranslatedPort);
    }
    public static String NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_PORT = "dest_translated_port";

    /**
     * The numbered Internet Protocol version.
     */
    public void setNetworkTrafficGenericIpVersion(
            int networkTrafficGenericIpVersion) {
        addField(NETWORK_TRAFFIC_GENERIC_IP_VERSION,
                networkTrafficGenericIpVersion);
    }
    public static String NETWORK_TRAFFIC_GENERIC_IP_VERSION = "ip_version";

    /**
     * The network interface through which a packet was transmitted.
     */
    public void setNetworkTrafficGenericOutboundInterface(
            String networkTrafficGenericOutboundInterface) {
        addField(NETWORK_TRAFFIC_GENERIC_OUTBOUND_INTERFACE,
                networkTrafficGenericOutboundInterface);
    }
    public static String NETWORK_TRAFFIC_GENERIC_OUTBOUND_INTERFACE = "outbound_interface";

    /**
     * How many packets this device/interface received.
     */
    public void setNetworkTrafficGenericPacketsIn(
            long networkTrafficGenericPacketsIn) {
        addField(NETWORK_TRAFFIC_GENERIC_PACKETS_IN,
                networkTrafficGenericPacketsIn);
    }
    public static String NETWORK_TRAFFIC_GENERIC_PACKETS_IN = "packets_in";

    /**
     * How many packets this device/interface transmitted.
     */
    public void setNetworkTrafficGenericPacketsOut(
            long networkTrafficGenericPacketsOut) {
        addField(NETWORK_TRAFFIC_GENERIC_PACKETS_OUT,
                networkTrafficGenericPacketsOut);
    }
    public static String NETWORK_TRAFFIC_GENERIC_PACKETS_OUT = "packets_out";

    /**
     * The OSI layer 3 (Network Layer) protocol, such as IPv4/IPv6, ICMP, IPsec,
     * IGMP or RIP.
     */
    public void setNetworkTrafficGenericProto(String networkTrafficGenericProto) {
        addField(NETWORK_TRAFFIC_GENERIC_PROTO, networkTrafficGenericProto);
    }
    public static String NETWORK_TRAFFIC_GENERIC_PROTO = "proto";

    /**
     * The session identifier. Multiple transactions build a session.
     */
    public void setNetworkTrafficGenericSessionId(
            String networkTrafficGenericSessionId) {
        addField(NETWORK_TRAFFIC_GENERIC_SESSION_ID,
                networkTrafficGenericSessionId);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SESSION_ID = "session_id";

    /**
     * The 802.11 service set identifier (ssid) assigned to a wireless session.
     */
    public void setNetworkTrafficGenericSsid(String networkTrafficGenericSsid) {
        addField(NETWORK_TRAFFIC_GENERIC_SSID, networkTrafficGenericSsid);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SSID = "ssid";

    /**
     * The country from which the packet was sent.
     */
    public void setNetworkTrafficGenericSrcCountry(
            String networkTrafficGenericSrcCountry) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_COUNTRY,
                networkTrafficGenericSrcCountry);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_COUNTRY = "src_country";

    /**
     * The fully qualified host name of the system that transmitted the packet.
     * For Web logs, this is the HTTP client.
     */
    public void setNetworkTrafficGenericSrcHost(
            String networkTrafficGenericSrcHost) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_HOST, networkTrafficGenericSrcHost);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_HOST = "src_host";

    /**
     * The interface that is listening locally or sending packets remotely.
     */
    public void setNetworkTrafficGenericSrcInt(
            String networkTrafficGenericSrcInt) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_INT, networkTrafficGenericSrcInt);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_INT = "src_int";

    /**
     * The IPv4 address of the packet's source. For Web logs, this is the http
     * client.
     */
    public void setNetworkTrafficGenericSrcIp(String networkTrafficGenericSrcIp) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_IP, networkTrafficGenericSrcIp);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_IP = "src_ip";

    /**
     * The IPv6 address of the packet's source.
     */
    public void setNetworkTrafficGenericSrcIpv6(
            String networkTrafficGenericSrcIpv6) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_IPV6, networkTrafficGenericSrcIpv6);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_IPV6 = "src_ipv6";

    /**
     * The (physical) latitude of the packet's source.
     */
    public void setNetworkTrafficGenericSrcLat(int networkTrafficGenericSrcLat) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_LAT, networkTrafficGenericSrcLat);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_LAT = "src_lat";

    /**
     * The (physical) longitude of the packet's source.
     */
    public void setNetworkTrafficGenericSrcLong(int networkTrafficGenericSrcLong) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_LONG, networkTrafficGenericSrcLong);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_LONG = "src_long";

    /**
     * The Media Access Control (MAC) address from which a packet was
     * transmitted.
     */
    public void setNetworkTrafficGenericSrcMac(
            String networkTrafficGenericSrcMac) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_MAC, networkTrafficGenericSrcMac);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_MAC = "src_mac";

    /**
     * The Windows NT domain containing the machines that generated the event.
     */
    public void setNetworkTrafficGenericSrcNtDomain(
            String networkTrafficGenericSrcNtDomain) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_NT_DOMAIN,
                networkTrafficGenericSrcNtDomain);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_NT_DOMAIN = "src_nt_domain";

    /**
     * The Windows NT hostname of the system that generated the event.
     */
    public void setNetworkTrafficGenericSrcNtHost(
            String networkTrafficGenericSrcNtHost) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_NT_HOST,
                networkTrafficGenericSrcNtHost);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_NT_HOST = "src_nt_host";

    /**
     * The network port from which a packet originated.
     */
    public void setNetworkTrafficGenericSrcPort(int networkTrafficGenericSrcPort) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_PORT, networkTrafficGenericSrcPort);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_PORT = "src_port";

    /**
     * The NATed IPv4 address from which a packet has been sent.
     */
    public void setNetworkTrafficGenericSrcTranslatedIp(
            String networkTrafficGenericSrcTranslatedIp) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_IP,
                networkTrafficGenericSrcTranslatedIp);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_IP = "src_translated_ip";

    /**
     * The NATed network port from which a packet has been sent.
     */
    public void setNetworkTrafficGenericSrcTranslatedPort(
            int networkTrafficGenericSrcTranslatedPort) {
        addField(NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_PORT,
                networkTrafficGenericSrcTranslatedPort);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_PORT = "src_translated_port";

    /**
     * The application, process, or OS subsystem that generated the event.
     */
    public void setNetworkTrafficGenericSyslogId(
            String networkTrafficGenericSyslogId) {
        addField(NETWORK_TRAFFIC_GENERIC_SYSLOG_ID,
                networkTrafficGenericSyslogId);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SYSLOG_ID = "syslog_id";

    /**
     * The criticality of an event, as recorded by UNIX syslog.
     */
    public void setNetworkTrafficGenericSyslogPriority(
            String networkTrafficGenericSyslogPriority) {
        addField(NETWORK_TRAFFIC_GENERIC_SYSLOG_PRIORITY,
                networkTrafficGenericSyslogPriority);
    }
    public static String NETWORK_TRAFFIC_GENERIC_SYSLOG_PRIORITY = "syslog_priority";

    /**
     * The TCP flag(s) specified in the event.
     */
    public void setNetworkTrafficGenericTcpFlag(
            String networkTrafficGenericTcpFlag) {
        addField(NETWORK_TRAFFIC_GENERIC_TCP_FLAG, networkTrafficGenericTcpFlag);
    }
    public static String NETWORK_TRAFFIC_GENERIC_TCP_FLAG = "tcp_flag";

    /**
     * The hex bit that specifies TCP 'type of service'
     * 
     * @see <a href="http://en.wikipedia.org/wiki/Type_of_Service">Type of
     *      Service</a>
     */
    public void setNetworkTrafficGenericTos(String networkTrafficGenericTos) {
        addField(NETWORK_TRAFFIC_GENERIC_TOS, networkTrafficGenericTos);
    }
    public static String NETWORK_TRAFFIC_GENERIC_TOS = "tos";

    /**
     * The transport protocol.
     */
    public void setNetworkTrafficGenericTransport(
            String networkTrafficGenericTransport) {
        addField(NETWORK_TRAFFIC_GENERIC_TRANSPORT,
                networkTrafficGenericTransport);
    }
    public static String NETWORK_TRAFFIC_GENERIC_TRANSPORT = "transport";

    /**
     * The "time to live" of a packet or datagram.
     */
    public void setNetworkTrafficGenericTtl(int networkTrafficGenericTtl) {
        addField(NETWORK_TRAFFIC_GENERIC_TTL, networkTrafficGenericTtl);
    }
    public static String NETWORK_TRAFFIC_GENERIC_TTL = "ttl";

    /**
     * The numeric identifier assigned to the virtual local area network (VLAN)
     * specified in the record.
     */
    public void setNetworkTrafficGenericVlanId(long networkTrafficGenericVlanId) {
        addField(NETWORK_TRAFFIC_GENERIC_VLAN_ID, networkTrafficGenericVlanId);
    }
    public static String NETWORK_TRAFFIC_GENERIC_VLAN_ID = "vlan_id";

    /**
     * The name assigned to the virtual local area network (VLAN) specified in
     * the record.
     */
    public void setNetworkTrafficGenericVlanName(
            String networkTrafficGenericVlanName) {
        addField(NETWORK_TRAFFIC_GENERIC_VLAN_NAME,
                networkTrafficGenericVlanName);
    }
    public static String NETWORK_TRAFFIC_GENERIC_VLAN_NAME = "vlan_name";


    // ----------------------------------
    // Packet filtering
    // ----------------------------------

    /**
     * The action the filtering device (the dvc_bestmatch field) performed on
     * the communication.
     */
    public void setPacketFilteringAction(String packetFilteringAction) {
        addField(PACKET_FILTERING_ACTION, packetFilteringAction);
    }
    public static String PACKET_FILTERING_ACTION = "action";

    /**
     * The IP port of the packet's destination, such as 22.
     */
    public void setPacketFilteringDestPort(int packetFilteringDestPort) {
        addField(PACKET_FILTERING_DEST_PORT, packetFilteringDestPort);
    }
    public static String PACKET_FILTERING_DEST_PORT = "dest_port";

    /**
     * The direction the packet is traveling.
     */
    public void setPacketFilteringDirection(String packetFilteringDirection) {
        addField(PACKET_FILTERING_DIRECTION, packetFilteringDirection);
    }
    public static String PACKET_FILTERING_DIRECTION = "direction";

    /**
     * The name of the packet filtering device. If your field is named dvc_host,
     * dvc_ip, or dvc_nt_host you can alias it as dvc to make it CIM-compliant.
     */
    public void setPacketFilteringDvc(String packetFilteringDvc) {
        addField(PACKET_FILTERING_DVC, packetFilteringDvc);
    }
    public static String PACKET_FILTERING_DVC = "dvc";

    /**
     * The rule which took action on the packet, such as 143.
     */
    public void setPacketFilteringRule(String packetFilteringRule) {
        addField(PACKET_FILTERING_RULE, packetFilteringRule);
    }
    public static String PACKET_FILTERING_RULE = "rule";

    /**
     * The IP port of the packet's source, such as 34541.
     */
    public void setPacketFilteringSvcPort(int packetFilteringSvcPort) {
        addField(PACKET_FILTERING_SVC_PORT, packetFilteringSvcPort);
    }
    public static String PACKET_FILTERING_SVC_PORT = "svc_port";


    // ----------------------------------
    // Proxy
    // ----------------------------------

    /**
     * The action taken by the proxy.
     */
    public void setProxyAction(String proxyAction) {
        addField(PROXY_ACTION, proxyAction);
    }
    public static String PROXY_ACTION = "action";

    /**
     * The destination of the network traffic (the remote host).
     */
    public void setProxyDest(String proxyDest) {
        addField(PROXY_DEST, proxyDest);
    }
    public static String PROXY_DEST = "dest";

    /**
     * The content-type of the requested HTTP resource.
     */
    public void setProxyHttpContentType(String proxyHttpContentType) {
        addField(PROXY_HTTP_CONTENT_TYPE, proxyHttpContentType);
    }
    public static String PROXY_HTTP_CONTENT_TYPE = "http_content_type";

    /**
     * The HTTP method used to request the resource.
     */
    public void setProxyHttpMethod(String proxyHttpMethod) {
        addField(PROXY_HTTP_METHOD, proxyHttpMethod);
    }
    public static String PROXY_HTTP_METHOD = "http_method";

    /**
     * The HTTP referrer used to request the HTTP resource.
     */
    public void setProxyHttpRefer(String proxyHttpRefer) {
        addField(PROXY_HTTP_REFER, proxyHttpRefer);
    }
    public static String PROXY_HTTP_REFER = "http_refer";

    /**
     * The HTTP response code.
     */
    public void setProxyHttpResponse(int proxyHttpResponse) {
        addField(PROXY_HTTP_RESPONSE, proxyHttpResponse);
    }
    public static String PROXY_HTTP_RESPONSE = "http_response";

    /**
     * The user agent used to request the HTTP resource.
     */
    public void setProxyHttpUserAgent(String proxyHttpUserAgent) {
        addField(PROXY_HTTP_USER_AGENT, proxyHttpUserAgent);
    }
    public static String PROXY_HTTP_USER_AGENT = "http_user_agent";

    /**
     * The product name of the vendor technology generating Network Protection
     * data, such as IDP, Providentia, and ASA.
     */
    public void setProxyProduct(String proxyProduct) {
        addField(PROXY_PRODUCT, proxyProduct);
    }
    public static String PROXY_PRODUCT = "product";

    /**
     * The source of the network traffic (the client requesting the connection).
     */
    public void setProxySrc(String proxySrc) {
        addField(PROXY_SRC, proxySrc);
    }
    public static String PROXY_SRC = "src";

    /**
     * The HTTP response code indicating the status of the proxy request.
     */
    public void setProxyStatus(int proxyStatus) {
        addField(PROXY_STATUS, proxyStatus);
    }
    public static String PROXY_STATUS = "status";

    /**
     * The user that requested the HTTP resource.
     */
    public void setProxyUser(String proxyUser) {
        addField(PROXY_USER, proxyUser);
    }
    public static String PROXY_USER = "user";

    /**
     * The URL of the requested HTTP resource.
     */
    public void setProxyUrl(String proxyUrl) {
        addField(PROXY_URL, proxyUrl);
    }
    public static String PROXY_URL = "url";

    /**
     * The vendor technology generating Network Protection data, such as IDP,
     * Providentia, and ASA.
     */
    public void setProxyVendor(String proxyVendor) {
        addField(PROXY_VENDOR, proxyVendor);
    }
    public static String PROXY_VENDOR = "vendor";


    // ----------------------------------
    // System center
    // ----------------------------------

    /**
     * The running application or service on the system (the src field), such as
     * explorer.exe or sshd.
     */
    public void setSystemCenterApp(String systemCenterApp) {
        addField(SYSTEM_CENTER_APP, systemCenterApp);
    }
    public static String SYSTEM_CENTER_APP = "app";

    /**
     * The amount of disk space available per drive or mount (the mount field)
     * on the system (the src field).
     */
    public void setSystemCenterFreembytes(long systemCenterFreembytes) {
        addField(SYSTEM_CENTER_FREEMBYTES, systemCenterFreembytes);
    }
    public static String SYSTEM_CENTER_FREEMBYTES = "FreeMBytes";

    /**
     * The version of operating system installed on the host (the src field),
     * such as 6.0.1.4 or 2.6.27.30-170.2.82.fc10.x86_64.
     */
    public void setSystemCenterKernelRelease(String systemCenterKernelRelease) {
        addField(SYSTEM_CENTER_KERNEL_RELEASE, systemCenterKernelRelease);
    }
    public static String SYSTEM_CENTER_KERNEL_RELEASE = "kernel_release";

    /**
     * Human-readable version of the SystemUptime value.
     */
    public void setSystemCenterLabel(String systemCenterLabel) {
        addField(SYSTEM_CENTER_LABEL, systemCenterLabel);
    }
    public static String SYSTEM_CENTER_LABEL = "label";

    /**
     * The drive or mount reporting available disk space (the FreeMBytes field)
     * on the system (the src field).
     */
    public void setSystemCenterMount(String systemCenterMount) {
        addField(SYSTEM_CENTER_MOUNT, systemCenterMount);
    }
    public static String SYSTEM_CENTER_MOUNT = "mount";

    /**
     * The name of the operating system installed on the host (the src), such as
     * Microsoft Windows Server 2003 or GNU/Linux).
     */
    public void setSystemCenterOs(String systemCenterOs) {
        addField(SYSTEM_CENTER_OS, systemCenterOs);
    }
    public static String SYSTEM_CENTER_OS = "os";

    /**
     * The percentage of processor utilization.
     */
    public void setSystemCenterPercentprocessortime(
            int systemCenterPercentprocessortime) {
        addField(SYSTEM_CENTER_PERCENTPROCESSORTIME,
                systemCenterPercentprocessortime);
    }
    public static String SYSTEM_CENTER_PERCENTPROCESSORTIME = "PercentProcessorTime";

    /**
     * The setlocaldefs setting from the SE Linux configuration.
     */
    public void setSystemCenterSetlocaldefs(int systemCenterSetlocaldefs) {
        addField(SYSTEM_CENTER_SETLOCALDEFS, systemCenterSetlocaldefs);
    }
    public static String SYSTEM_CENTER_SETLOCALDEFS = "setlocaldefs";

    /**
     * Values from the SE Linux configuration file.
     */
    public void setSystemCenterSelinux(String systemCenterSelinux) {
        addField(SYSTEM_CENTER_SELINUX, systemCenterSelinux);
    }
    public static String SYSTEM_CENTER_SELINUX = "selinux";

    /**
     * The SE Linux type (such as targeted).
     */
    public void setSystemCenterSelinuxtype(String systemCenterSelinuxtype) {
        addField(SYSTEM_CENTER_SELINUXTYPE, systemCenterSelinuxtype);
    }
    public static String SYSTEM_CENTER_SELINUXTYPE = "selinuxtype";

    /**
     * The shell provided to the User Account (the user field) upon logging into
     * the system (the src field).
     */
    public void setSystemCenterShell(String systemCenterShell) {
        addField(SYSTEM_CENTER_SHELL, systemCenterShell);
    }
    public static String SYSTEM_CENTER_SHELL = "shell";

    /**
     * The TCP/UDP source port on the system (the src field).
     */
    public void setSystemCenterSrcPort(int systemCenterSrcPort) {
        addField(SYSTEM_CENTER_SRC_PORT, systemCenterSrcPort);
    }
    public static String SYSTEM_CENTER_SRC_PORT = "src_port";

    /**
     * The sshd protocol version.
     */
    public void setSystemCenterSshdProtocol(String systemCenterSshdProtocol) {
        addField(SYSTEM_CENTER_SSHD_PROTOCOL, systemCenterSshdProtocol);
    }
    public static String SYSTEM_CENTER_SSHD_PROTOCOL = "sshd_protocol";

    /**
     * The start mode of the given service.
     */
    public void setSystemCenterStartmode(String systemCenterStartmode) {
        addField(SYSTEM_CENTER_STARTMODE, systemCenterStartmode);
    }
    public static String SYSTEM_CENTER_STARTMODE = "Startmode";

    /**
     * The number of seconds since the system (the src) has been "up."
     */
    public void setSystemCenterSystemuptime(long systemCenterSystemuptime) {
        addField(SYSTEM_CENTER_SYSTEMUPTIME, systemCenterSystemuptime);
    }
    public static String SYSTEM_CENTER_SYSTEMUPTIME = "SystemUptime";

    /**
     * The total amount of available memory on the system (the src).
     */
    public void setSystemCenterTotalmbytes(long systemCenterTotalmbytes) {
        addField(SYSTEM_CENTER_TOTALMBYTES, systemCenterTotalmbytes);
    }
    public static String SYSTEM_CENTER_TOTALMBYTES = "TotalMBytes";

    /**
     * The amount of used memory on the system (the src).
     */
    public void setSystemCenterUsedmbytes(long systemCenterUsedmbytes) {
        addField(SYSTEM_CENTER_USEDMBYTES, systemCenterUsedmbytes);
    }
    public static String SYSTEM_CENTER_USEDMBYTES = "UsedMBytes";

    /**
     * The User Account present on the system (the src).
     */
    public void setSystemCenterUser(String systemCenterUser) {
        addField(SYSTEM_CENTER_USER, systemCenterUser);
    }
    public static String SYSTEM_CENTER_USER = "user";

    /**
     * The number of updates the system (the src) is missing.
     */
    public void setSystemCenterUpdates(long systemCenterUpdates) {
        addField(SYSTEM_CENTER_UPDATES, systemCenterUpdates);
    }
    public static String SYSTEM_CENTER_UPDATES = "updates";


    // ----------------------------------
    // Traffic
    // ----------------------------------

    /**
     * The destination of the network traffic. If your field is named dest_host,
     * dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest to make it
     * CIM-compliant.
     */
    public void setTrafficDest(String trafficDest) {
        addField(TRAFFIC_DEST, trafficDest);
    }
    public static String TRAFFIC_DEST = "dest";

    /**
     * The name of the packet filtering device. If your field is named dvc_host,
     * dvc_ip, or dvc_nt_host you can alias it as dvc to make it CIM-compliant.
     */
    public void setTrafficDvc(String trafficDvc) {
        addField(TRAFFIC_DVC, trafficDvc);
    }
    public static String TRAFFIC_DVC = "dvc";

    /**
     * The source of the network traffic. If your field is named src_host,
     * src_ip, src_ipv6, or src_nt_host you can alias it as src to make it
     * CIM-compliant.
     */
    public void setTrafficSrc(String trafficSrc) {
        addField(TRAFFIC_SRC, trafficSrc);
    }
    public static String TRAFFIC_SRC = "src";


    // ----------------------------------
    // Update
    // ----------------------------------

    /**
     * The name of the installed update.
     */
    public void setUpdatePackage(String updatePackage) {
        addField(UPDATE_PACKAGE, updatePackage);
    }
    public static String UPDATE_PACKAGE = "package";


    // ----------------------------------
    // User information updates
    // ----------------------------------

    /**
     * A user that has been affected by a change. For example, user fflanda
     * changed the name of user rhallen, so affected_user=rhallen.
     */
    public void setUserInfoUpdatesAffectedUser(
            String userInfoUpdatesAffectedUser) {
        addField(USER_INFO_UPDATES_AFFECTED_USER, userInfoUpdatesAffectedUser);
    }
    public static String USER_INFO_UPDATES_AFFECTED_USER = "affected_user";

    /**
     * The user group affected by a change.
     */
    public void setUserInfoUpdatesAffectedUserGroup(
            String userInfoUpdatesAffectedUserGroup) {
        addField(USER_INFO_UPDATES_AFFECTED_USER_GROUP,
                userInfoUpdatesAffectedUserGroup);
    }
    public static String USER_INFO_UPDATES_AFFECTED_USER_GROUP = "affected_user_group";

    /**
     * The identifier of the user group affected by a change.
     */
    public void setUserInfoUpdatesAffectedUserGroupId(
            int userInfoUpdatesAffectedUserGroupId) {
        addField(USER_INFO_UPDATES_AFFECTED_USER_GROUP_ID,
                userInfoUpdatesAffectedUserGroupId);
    }
    public static String USER_INFO_UPDATES_AFFECTED_USER_GROUP_ID = "affected_user_group_id";

    /**
     * The identifier of the user affected by a change.
     */
    public void setUserInfoUpdatesAffectedUserId(
            int userInfoUpdatesAffectedUserId) {
        addField(USER_INFO_UPDATES_AFFECTED_USER_ID,
                userInfoUpdatesAffectedUserId);
    }
    public static String USER_INFO_UPDATES_AFFECTED_USER_ID = "affected_user_id";

    /**
     * The security context associated with the user affected by a change.
     */
    public void setUserInfoUpdatesAffectedUserPrivilege(
            String userInfoUpdatesAffectedUserPrivilege) {
        addField(USER_INFO_UPDATES_AFFECTED_USER_PRIVILEGE,
                userInfoUpdatesAffectedUserPrivilege);
    }
    public static String USER_INFO_UPDATES_AFFECTED_USER_PRIVILEGE = "affected_user_privilege";

    /**
     * The name of the user affected by the recorded event.
     */
    public void setUserInfoUpdatesUser(String userInfoUpdatesUser) {
        addField(USER_INFO_UPDATES_USER, userInfoUpdatesUser);
    }
    public static String USER_INFO_UPDATES_USER = "user";

    /**
     * A user group that is the object of an event, expressed in human-readable
     * terms.
     */
    public void setUserInfoUpdatesUserGroup(String userInfoUpdatesUserGroup) {
        addField(USER_INFO_UPDATES_USER_GROUP, userInfoUpdatesUserGroup);
    }
    public static String USER_INFO_UPDATES_USER_GROUP = "user_group";

    /**
     * The numeric identifier assigned to the user group event object.
     */
    public void setUserInfoUpdatesUserGroupId(int userInfoUpdatesUserGroupId) {
        addField(USER_INFO_UPDATES_USER_GROUP_ID, userInfoUpdatesUserGroupId);
    }
    public static String USER_INFO_UPDATES_USER_GROUP_ID = "user_group_id";

    /**
     * The system-assigned identifier for the user affected by an event.
     */
    public void setUserInfoUpdatesUserId(int userInfoUpdatesUserId) {
        addField(USER_INFO_UPDATES_USER_ID, userInfoUpdatesUserId);
    }
    public static String USER_INFO_UPDATES_USER_ID = "user_id";

    /**
     * The security context associated with the object of an event (the affected
     * user).
     */
    public void setUserInfoUpdatesUserPrivilege(
            String userInfoUpdatesUserPrivilege) {
        addField(USER_INFO_UPDATES_USER_PRIVILEGE, userInfoUpdatesUserPrivilege);
    }
    public static String USER_INFO_UPDATES_USER_PRIVILEGE = "user_privilege";

    /**
     * The name of the user that is the subject of an event--the user executing
     * the action, in other words.
     */
    public void setUserInfoUpdatesUserSubject(String userInfoUpdatesUserSubject) {
        addField(USER_INFO_UPDATES_USER_SUBJECT, userInfoUpdatesUserSubject);
    }
    public static String USER_INFO_UPDATES_USER_SUBJECT = "user_subject";

    /**
     * The ID number of the user that is the subject of an event.
     */
    public void setUserInfoUpdatesUserSubjectId(int userInfoUpdatesUserSubjectId) {
        addField(USER_INFO_UPDATES_USER_SUBJECT_ID, userInfoUpdatesUserSubjectId);
    }
    public static String USER_INFO_UPDATES_USER_SUBJECT_ID = "user_subject_id";

    /**
     * The security context associated with the subject of an event (the user
     * causing a change).
     */
    public void setUserInfoUpdatesUserSubjectPrivilege(
            String userInfoUpdatesUserSubjectPrivilege) {
        addField(USER_INFO_UPDATES_USER_SUBJECT_PRIVILEGE,
                userInfoUpdatesUserSubjectPrivilege);
    }
    public static String USER_INFO_UPDATES_USER_SUBJECT_PRIVILEGE = "user_subject_privilege";


    // ----------------------------------
    // Vulnerability
    // ----------------------------------

    /**
     * The category of the discovered vulnerability.
     */
    public void setVulnerabilityCategory(String vulnerabilityCategory) {
        addField(VULNERABILITY_CATEGORY, vulnerabilityCategory);
    }
    public static String VULNERABILITY_CATEGORY = "category";

    /**
     * The host with the discovered vulnerability. If your field is named
     * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
     * to make it CIM-compliant.
     */
    public void setVulnerabilityDest(String vulnerabilityDest) {
        addField(VULNERABILITY_DEST, vulnerabilityDest);
    }
    public static String VULNERABILITY_DEST = "dest";

    /**
     * The operating system of the host containing the vulnerability detected on
     * the client (the src field), such as SuSE Security Update, or cups
     * security update.
     */
    public void setVulnerabilityOs(String vulnerabilityOs) {
        addField(VULNERABILITY_OS, vulnerabilityOs);
    }
    public static String VULNERABILITY_OS = "os";

    /**
     * The severity of the discovered vulnerability.
     */
    public void setVulnerabilitySeverity(String vulnerabilitySeverity) {
        addField(VULNERABILITY_SEVERITY, vulnerabilitySeverity);
    }
    public static String VULNERABILITY_SEVERITY = "severity";

    /**
     * The name of the vulnerability detected on the client (the src field),
     * such as SuSE Security Update, or cups security update.
     */
    public void setVulnerabilitySignature(String vulnerabilitySignature) {
        addField(VULNERABILITY_SIGNATURE, vulnerabilitySignature);
    }
    public static String VULNERABILITY_SIGNATURE = "signature";


    // ----------------------------------
    // Windows administration
    // ----------------------------------

    /**
     * The object name (associated only with Windows).
     */
    public void setWindowsAdminObjectName(String windowsAdminObjectName) {
        addField(WINDOWS_ADMIN_OBJECT_NAME, windowsAdminObjectName);
    }
    public static String WINDOWS_ADMIN_OBJECT_NAME = "object_name";

    /**
     * The object type (associated only with Windows).
     */
    public void setWindowsAdminObjectType(String windowsAdminObjectType) {
        addField(WINDOWS_ADMIN_OBJECT_TYPE, windowsAdminObjectType);
    }
    public static String WINDOWS_ADMIN_OBJECT_TYPE = "object_type";

    /**
     * The object handle (associated only with Windows).
     */
    public void setWindowsAdminObjectHandle(String windowsAdminObjectHandle) {
        addField(WINDOWS_ADMIN_OBJECT_HANDLE, windowsAdminObjectHandle);
    }
    public static String WINDOWS_ADMIN_OBJECT_HANDLE = "object_handle";
}
