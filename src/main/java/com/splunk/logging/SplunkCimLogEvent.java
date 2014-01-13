package com.splunk.logging;

import java.util.Date;
import java.util.LinkedHashMap;

/**
 * <pre>
 * This is a class to encapsulate a Splunk Log Event Message using Splunk best practice logging semantics.
 * It is based on standard Splunk CIM(Common Information Model) fields.
 * You can also add any of your own custom fields as you require.
 * <br/>
 * You will most likely use this class to format a message to pass to your logging framework (log4j, java.util logging, logback etc..).
 * 
 * <code>
 * Logger logger = LoggerFactory.getLogger("splunk.logger");
 * SplunkCimLogEvent event = new SplunkCimLogEvent("Failed Login","sshd:failure");
 * event.setAuthApp("jane");
 * event.setAuthUser("jane");
 * event.addField("somefieldname", "foobar");
 * logger.info(event.toString());
 * </code>
 * 
 * The underlying log framework will also have a grammar for declaring a log message pattern.
 * Therefore you can either just log the SplunkCimLogEvent string as is, or augment it with other log pattern pattern variables when configuring your logger appenders/handlers.
 * 
 * </pre>
 * 
 * @see <a
 *      href="http://docs.splunk.com/Documentation/Splunk/latest/Knowledge/UnderstandandusetheCommonInformationModel">Splunk
 *      CIM</a>
 * @see <a
 *      href="http://dev.splunk.com/view/logging-best-practices/SP-CAAADP6">Splunk
 *      Logging Best Practices</a>
 * 
 * 
 * @author Damien Dallimore damien@dtdsoftware.com
 * 
 */
public class SplunkCimLogEvent {
    /**
     * Delimiters to use in formatting the event.
     */
	private static final String KVDELIM = "=";
	private static final String PAIRDELIM = " ";
	private static final char QUOTE = '"';

    LinkedHashMap<String, Object> entries;

    /**
     * Constructor.
     *
     * @param eventName
     *            the event name
     * @param eventID
     *            the event id
     */
    public SplunkCimLogEvent(String eventName, String eventID) {
        entries = new LinkedHashMap<String, Object>();

        addField(PREFIX_NAME, eventName);
        addField(PREFIX_EVENT_ID, eventID);
    }

    /**
     * Add a key value pair
     *
     * @param key
     * @param value
     */
    public void addField(String key, Object value) {
        entries.put(key, value);
    }

    /**
     * Utility method for formatting Throwable,Error,Exception objects in a more
     * linear and Splunk friendly manner than printStackTrace
     *
     * @param throwable
     *            the Throwable object to add to the event
     * @param stackTraceDepth
     *            maximum number of stacktrace elements to log
     */
    public void addThrowableWithStacktrace(Throwable throwable) {

        addThrowableWithStacktrace(throwable, -1);
    }

    /**
     * Internal private method for formatting Throwable,Error,Exception objects
     * in a more linear and Splunk friendly manner than printStackTrace
     *
     * @param throwable
     *            the Throwable object to add to the event
     * @param stackTraceDepth
     *            maximum number of stacktrace elements to log, -1 for all
     */

    private void addThrowableWithStacktrace(Throwable throwable, int stackTraceDepth) {

        addField(THROWABLE_CLASS, throwable.getClass().getCanonicalName());
        addField(THROWABLE_MESSAGE, throwable.getMessage());
        StackTraceElement[] elements = throwable.getStackTrace();
        StringBuffer sb = new StringBuffer();
        int depth = 0;
        for (StackTraceElement element : elements) {
            depth++;
            if (stackTraceDepth == -1 || stackTraceDepth >= depth)
                sb.append(element.toString()).append(",");
            else
                break;

        }
        addField(THROWABLE_STACKTRACE_ELEMENTS, sb.toString());
    }

    @Override
    /**
     * return the completed event message
     */
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
            output.append(QUOTE + key + "=" + value + QUOTE);
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
	public static String AC_MANAGEMENT_DEST_NT_DOMAIN = "dest_nt_domain";
    public void setAcManagementDestNtDomain(String acManagementDestNtDomain) {
        addField(AC_MANAGEMENT_DEST_NT_DOMAIN, acManagementDestNtDomain);
    }

    /**
	 * Description of the account management change performed.
	 */
	public static String AC_MANAGEMENT_SIGNATURE = "signature";
    public void setAcManagementSignature(String acManagementSignature) {
        addField(AC_MANAGEMENT_SIGNATURE, acManagementSignature);
    }

    /**
	 * The NT source of the destination. In the case of an account management
	 * event, this is the domain that contains the user that generated the
	 * event.
	 */
	public static String AC_MANAGEMENT_SRC_NT_DOMAIN = "src_nt_domain";
    public void setAcManagementSrcNtDomain(String acManagementSrcNtDomain) {
        addField(AC_MANAGEMENT_SRC_NT_DOMAIN, acManagementSrcNtDomain);
    }

    // ----------------------------------
	// Authentication - Access protection
	// ----------------------------------

	/**
	 * The action performed on the resource. success, failure
	 */
	public static String AUTH_ACTION = "action";
    public void setAuthAction(String authAction) {
        addField(AUTH_ACTION, authAction);
    }
	/**
	 * The application involved in the event (such as ssh, spunk, win:local).
	 */
	public static String AUTH_APP = "app";
    public void setAuthApp(String authApp) {
        addField(AUTH_APP, authApp);
    }

    /**
	 * The target involved in the authentication. If your field is named
	 * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
	 * to make it CIM-compliant.
	 */
	public static String AUTH_DEST = "dest";
    public void setAuthDest(String authDest) {
        addField(AUTH_DEST, authDest);
    }

    /**
	 * The source involved in the authentication. In the case of endpoint
	 * protection authentication the src is the client. If your field is named
	 * src_host, src_ip, src_ipv6, or src_nt_host you can alias it as src to
	 * make it CIM-compliant.. It is required for all events dealing with
	 * endpoint protection (Authentication, change analysis, malware, system
	 * center, and update). Note: Do not confuse this with the event source or
	 * sourcetype fields.
	 */
	public static String AUTH_SRC = "src";
    public void setAuthSrc(String authSrc) {
        addField(AUTH_SRC, authSrc);
    }

	/**
	 * In privilege escalation events, src_user represents the user who
	 * initiated the privilege escalation.
	 */
	public static String AUTH_SRC_USER = "src_user";
    public void setAuthSrcUser(String authSrcUser) {
        addField(AUTH_SRC_USER, authSrcUser);
    }

	/**
	 * The name of the user involved in the event, or who initiated the event.
	 * For authentication privilege escalation events this should represent the
	 * user targeted by the escalation.
	 */
	public static String AUTH_USER = "user";
    public void setAuthUser(String authUser) {
        addField(AUTH_USER, authUser);
    }

	// ----------------------------------
	// Change analysis - Endpoint protection
	// ----------------------------------

	/**
	 * The action performed on the resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_ACTION = "action";
    public void setChangeEndpointProtectionAction(
            String changeEndpointProtectionAction) {
        addField(CHANGE_ENDPOINT_PROTECTION_ACTION,
                changeEndpointProtectionAction);
    }

	/**
	 * The type of change discovered in the change analysis event.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_CHANGE_TYPE = "change_type";
    public void setChangeEndpointProtectionChangeType(
            String changeEndpointProtectionChangeType) {
        addField(CHANGE_ENDPOINT_PROTECTION_CHANGE_TYPE,
                changeEndpointProtectionChangeType);
    }

	/**
	 * The host that was affected by the change. If your field is named
	 * dest_host,dest_ip,dest_ipv6, or dest_nt_host you can alias it as dest to
	 * make it CIM-compliant.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_DEST = "dest";
    public void setChangeEndpointProtectionDest(
            String changeEndpointProtectionDest) {
        addField(CHANGE_ENDPOINT_PROTECTION_DEST, changeEndpointProtectionDest);
    }

	/**
	 * The hash signature of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_HASH = "hash";
    public void setChangeEndpointProtectionHash(
            String changeEndpointProtectionHash) {
        addField(CHANGE_ENDPOINT_PROTECTION_HASH, changeEndpointProtectionHash);
    }

	/**
	 * The group ID of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_GID = "gid";
    public void setChangeEndpointProtectionGid(long changeEndpointProtectionGid) {
        addField(CHANGE_ENDPOINT_PROTECTION_GID, changeEndpointProtectionGid);
    }

	/**
	 * Indicates whether or not the modified resource is a directory.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_ISDR = "isdr";
    public void setChangeEndpointProtectionIsdr(
            boolean changeEndpointProtectionIsdr) {
        addField(CHANGE_ENDPOINT_PROTECTION_ISDR, changeEndpointProtectionIsdr);
    }

	/**
	 * The permissions mode of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_MODE = "mode";
    public void setChangeEndpointProtectionMode(
            long changeEndpointProtectionMode) {
        addField(CHANGE_ENDPOINT_PROTECTION_MODE, changeEndpointProtectionMode);
    }

    /**
	 * The modification time of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_MODTIME = "modtime";
    public void setChangeEndpointProtectionModtime(
            String changeEndpointProtectionModtime) {
        addField(CHANGE_ENDPOINT_PROTECTION_MODTIME,
                changeEndpointProtectionModtime);
    }

    /**
	 * The file path of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_PATH = "path";
    public void setChangeEndpointProtectionPath(
            String changeEndpointProtectionPath) {
        addField(CHANGE_ENDPOINT_PROTECTION_PATH, changeEndpointProtectionPath);
    }

	/**
	 * The size of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_SIZE = "size";
    public void setChangeEndpointProtectionSize(
            long changeEndpointProtectionSize) {
        addField(CHANGE_ENDPOINT_PROTECTION_SIZE, changeEndpointProtectionSize);
    }

	/**
	 * The user ID of the modified resource.
	 */
	public static String CHANGE_ENDPOINT_PROTECTION_UID = "uid";
    public void setChangeEndpointProtectionUid(long changeEndpointProtectionUid) {
        addField(CHANGE_ENDPOINT_PROTECTION_UID, changeEndpointProtectionUid);
    }

    // ----------------------------------
	// Change analysis - Network protection
	// ----------------------------------

	/**
	 * The type of change observed.
	 */
	public static String CHANGE_NETWORK_PROTECTION_ACTION = "action";
    public void setChangeNetworkProtectionAction(
            String changeNetworkProtectionAction) {
        addField(CHANGE_NETWORK_PROTECTION_ACTION, changeNetworkProtectionAction);
    }

	/**
	 * The command that initiated the change.
	 */
	public static String CHANGE_NETWORK_PROTECTION_COMMAND = "command";
    public void setChangeNetworkProtectionCommand(
            String changeNetworkProtectionCommand) {
        addField(CHANGE_NETWORK_PROTECTION_COMMAND,
                changeNetworkProtectionCommand);
    }

	/**
	 * The device that is directly affected by the change.
	 */
	public static String CHANGE_NETWORK_PROTECTION_DVC = "dvc";
    public void setChangeNetworkProtectionDvc(String changeNetworkProtectionDvc) {
        addField(CHANGE_NETWORK_PROTECTION_DVC, changeNetworkProtectionDvc);
    }

    /**
	 * The user that initiated the change.
	 */
	public static String CHANGE_NETWORK_PROTECTION_USER = "user";
    public void setChangeNetworkProtectionUser(
            String changeNetworkProtectionUser) {
        addField(CHANGE_NETWORK_PROTECTION_USER, changeNetworkProtectionUser);
    }

	// ----------------------------------
	// Common event fields
	// ----------------------------------

	/**
	 * A device-specific classification provided as part of the event.
	 */
	public static String COMMON_CATEGORY = "category";
    public void setCommonCategory(String commonCategory) {
        addField(COMMON_CATEGORY, commonCategory);
    }

	/**
	 * A device-specific classification provided as part of the event.
	 */
	public static String COMMON_COUNT = "count";
    public void setCommonCount(String commonCount) {
        addField(COMMON_COUNT, commonCount);
    }

	/**
	 * The free-form description of a particular event.
	 */
	public static String COMMON_DESC = "desc";
    public void setCommonDesc(String commonDesc) {
        addField(COMMON_DESC, commonDesc);
    }

	/**
	 * The name of a given DHCP pool on a DHCP server.
	 */
	public static String COMMON_DHCP_POOL = "dhcp_pool";
    public void setCommonDhcpPool(String commonDhcpPool) {
        addField(COMMON_DHCP_POOL, commonDhcpPool);
    }

	/**
	 * The amount of time the event lasted.
	 */
	public static String COMMON_DURATION = "duration";
    public void setCommonDuration(long commonDuration) {
        addField(COMMON_DURATION, commonDuration);
    }

	/**
	 * The fully qualified domain name of the device transmitting or recording
	 * the log record.
	 */
	public static String COMMON_DVC_HOST = "dvc_host";
    public void setCommonDvcHost(String commonDvcHost) {
        addField(COMMON_DVC_HOST, commonDvcHost);
    }

    /**
	 * The IPv4 address of the device reporting the event.
	 */
	public static String COMMON_DVC_IP = "dvc_ip";
    public void setCommonDvcIp(String commonDvcIp) {
        addField(COMMON_DVC_IP, commonDvcIp);
    }

	/**
	 * The IPv6 address of the device reporting the event.
	 */
	public static String COMMON_DVC_IP6 = "dvc_ip6";
    public void setCommonDvcIp6(String commonDvcIp6) {
        addField(COMMON_DVC_IP6, commonDvcIp6);
    }

	/**
	 * The free-form description of the device's physical location.
	 */
	public static String COMMON_DVC_LOCATION = "dvc_location";
    public void setCommonDvcLocation(String commonDvcLocation) {
        addField(COMMON_DVC_LOCATION, commonDvcLocation);
    }

	/**
	 * The MAC (layer 2) address of the device reporting the event.
	 */
	public static String COMMON_DVC_MAC = "dvc_mac";
    public void setCommonDvcMac(String commonDvcMac) {
        addField(COMMON_DVC_MAC, commonDvcMac);
    }

	/**
	 * The Windows NT domain of the device recording or transmitting the event.
	 */
	public static String COMMON_DVC_NT_DOMAIN = "dvc_nt_domain";
    public void setCommonDvcNtDomain(String commonDvcNtDomain) {
        addField(COMMON_DVC_NT_DOMAIN, commonDvcNtDomain);
    }

	/**
	 * The Windows NT host name of the device recording or transmitting the
	 * event.
	 */
	public static String COMMON_DVC_NT_HOST = "dvc_nt_host";
    public void setCommonDvcNtHost(String commonDvcNtHost) {
        addField(COMMON_DVC_NT_HOST, commonDvcNtHost);
    }

	/**
	 * Time at which the device recorded the event.
	 */
	public static String COMMON_DVC_TIME = "dvc_time";
    public void setCommonDvcTime(long commonDvcTime) {
        addField(COMMON_DVC_TIME, commonDvcTime);
    }

	/**
	 * The event's specified end time.
	 */
	public static String COMMON_END_TIME = "end_time";
    public void setCommonEndTime(long commonEndTime) {
        addField(COMMON_END_TIME, commonEndTime);
    }

	/**
	 * A unique identifier that identifies the event. This is unique to the
	 * reporting device.
	 */
	public static String COMMON_EVENT_ID = "event_id";
    public void setCommonEventId(long commonEventId) {
        addField(COMMON_EVENT_ID, commonEventId);
    }

	/**
	 * The length of the datagram, event, message, or packet.
	 */
	public static String COMMON_LENGTH = "length";
    public void setCommonLength(long commonLength) {
        addField(COMMON_LENGTH, commonLength);
    }

	/**
	 * The log-level that was set on the device and recorded in the event.
	 */
	public static String COMMON_LOG_LEVEL = "log_level";
    public void setCommonLogLevel(String commonLogLevel) {
        addField(COMMON_LOG_LEVEL, commonLogLevel);
    }

	/**
	 * The name of the event as reported by the device. The name should not
	 * contain information that's already being parsed into other fields from
	 * the event, such as IP addresses.
	 */
	public static String COMMON_NAME = "name";
    public void setCommonName(String commonName) {
        addField(COMMON_NAME, commonName);
    }

	/**
	 * An integer assigned by the device operating system to the process
	 * creating the record.
	 */
	public static String COMMON_PID = "pid";
    public void setCommonPid(long commonPid) {
        addField(COMMON_PID, commonPid);
    }

	/**
	 * An environment-specific assessment of the event's importance, based on
	 * elements such as event severity, business function of the affected
	 * system, or other locally defined variables.
	 */
	public static String COMMON_PRIORITY = "priority";
    public void setCommonPriority(long commonPriority) {
        addField(COMMON_PRIORITY, commonPriority);
    }

	/**
	 * The product that generated the event.
	 */
	public static String COMMON_PRODUCT = "product";
    public void setCommonProduct(String commonProduct) {
        addField(COMMON_PRODUCT, commonProduct);
    }

	/**
	 * The version of the product that generated the event.
	 */
	public static String COMMON_PRODUCT_VERSION = "product_version";
    public void setCommonProductVersion(long commonProductVersion) {
        addField(COMMON_PRODUCT_VERSION, commonProductVersion);
    }

	/**
	 * The result root cause, such as connection refused, timeout, crash, and so
	 * on.
	 */
	public static String COMMON_REASON = "reason";
    public void setCommonReason(String commonReason) {
        addField(COMMON_REASON, commonReason);
    }

	/**
	 * The action result. Often is a binary choice: succeeded and failed,
	 * allowed and denied, and so on.
	 */
	public static String COMMON_RESULT = "result";
    public void setCommonResult(String commonResult) {
        addField(COMMON_RESULT, commonResult);
    }

	/**
	 * The severity (or priority) of an event as reported by the originating
	 * device.
	 */
	public static String COMMON_SEVERITY = "severity";
    public void setCommonSeverity(String commonSeverity) {
        addField(COMMON_SEVERITY, commonSeverity);
    }

	/**
	 * The event's specified start time.
	 */
	public static String COMMON_START_TIME = "start_time";
    public void setCommonStartTime(long commonStartTime) {
        addField(COMMON_START_TIME, commonStartTime);
    }

	/**
	 * The transaction identifier.
	 */
	public static String COMMON_TRANSACTION_ID = "transaction_id";
    public void setCommonTransactionId(String commonTransactionId) {
        addField(COMMON_TRANSACTION_ID, commonTransactionId);
    }

	/**
	 * A uniform record locator (a web address, in other words) included in a
	 * record.
	 */
	public static String COMMON_URL = "url";
    public void setCommonUrl(String commonUrl) {
        addField(COMMON_URL, commonUrl);
    }

	/**
	 * The vendor who made the product that generated the event.
	 */
	public static String COMMON_VENDOR = "vendor";
    public void setCommonVendor(String commonVendor) {
        addField(COMMON_VENDOR, commonVendor);
    }

	// ----------------------------------
	// DNS protocol
	// ----------------------------------

	/**
	 * The DNS domain that has been queried.
	 */
	public static String DNS_DEST_DOMAIN = "dest_domain";
    public void setDnsDestDomain(String dnsDestDomain) {
        addField(DNS_DEST_DOMAIN, dnsDestDomain);
    }

	/**
	 * The remote DNS resource record being acted upon.
	 */
	public static String DNS_DEST_RECORD = "dest_record";
    public void setDnsDestRecord(String dnsDestRecord) {
        addField(DNS_DEST_RECORD, dnsDestRecord);
    }

    /**
	 * The DNS zone that is being received by the slave as part of a zone
	 * transfer.
	 */
	public static String DNS_DEST_ZONE = "dest_zone";
    public void setDnsDestZone(String dnsDestZone) {
        addField(DNS_DEST_ZONE, dnsDestZone);
    }

	/**
	 * The DNS resource record class.
	 */
	public static String DNS_RECORD_CLASS = "record_class";
    public void setDnsRecordClass(String dnsRecordClass) {
        addField(DNS_RECORD_CLASS, dnsRecordClass);
    }

	/**
	 * The DNS resource record type.
	 * 
	 * @see <a
	 *      href="https://secure.wikimedia.org/wikipedia/en/wiki/List_of_DNS_record_types">see
	 *      this Wikipedia article on DNS record types</a>
	 */
	public static String DNS_RECORD_TYPE = "record_type";
    public void setDnsRecordType(String dnsRecordType) {
        addField(DNS_RECORD_TYPE, dnsRecordType);
    }

	/**
	 * The local DNS domain that is being queried.
	 */
	public static String DNS_SRC_DOMAIN = "src_domain";
    public void setDnsSrcDomain(String dnsSrcDomain) {
        addField(DNS_SRC_DOMAIN, dnsSrcDomain);
    }

	/**
	 * The local DNS resource record being acted upon.
	 */
	public static String DNS_SRC_RECORD = "src_record";
    public void setDnsSrcRecord(String dnsSrcRecord) {
        addField(DNS_SRC_RECORD, dnsSrcRecord);
    }

	/**
	 * The DNS zone that is being transferred by the master as part of a zone
	 * transfer.
	 */
	public static String DNS_SRC_ZONE = "src_zone";
    public void setDnsSrcZone(String dnsSrcZone) {
        addField(DNS_SRC_ZONE, dnsSrcZone);
    }

	// ----------------------------------
	// Email tracking
	// ----------------------------------

	/**
	 * The person to whom an email is sent.
	 */
	public static String EMAIL_RECIPIENT = "recipient";
    public void setEmailRecipient(String emailRecipient) {
        addField(EMAIL_RECIPIENT, emailRecipient);
    }

	/**
	 * The person responsible for sending an email.
	 */
	public static String EMAIL_SENDER = "sender";
    public void setEmailSender(String emailSender) {
        addField(EMAIL_SENDER, emailSender);
    }

	/**
	 * The email subject line.
	 */
	public static String EMAIL_SUBJECT = "subject";
    public void setEmailSubject(String emailSubject) {
        addField(EMAIL_SUBJECT, emailSubject);
    }

	// ----------------------------------
	// File management
	// ----------------------------------

	/**
	 * The time the file (the object of the event) was accessed.
	 */
	public static String FILE_ACCESS_TIME = "file_access_time";
    public void setFileAccessTime(long fileAccessTime) {
        addField(FILE_ACCESS_TIME, fileAccessTime);
    }

	/**
	 * The time the file (the object of the event) was created.
	 */
	public static String FILE_CREATE_TIME = "file_create_time";
    public void setFileCreateTime(long fileCreateTime) {
        addField(FILE_CREATE_TIME, fileCreateTime);
    }

	/**
	 * A cryptographic identifier assigned to the file object affected by the
	 * event.
	 */
	public static String FILE_HASH = "file_hash";
    public void setFileHash(String fileHash) {
        addField(FILE_HASH, fileHash);
    }

	/**
	 * The time the file (the object of the event) was altered.
	 */
	public static String FILE_MODIFY_TIME = "file_modify_time";
    public void setFileModifyTime(long fileModifyTime) {
        addField(FILE_MODIFY_TIME, fileModifyTime);
    }

	/**
	 * The name of the file that is the object of the event (without location
	 * information related to local file or directory structure).
	 */
	public static String FILE_NAME = "file_name";
    public void setFileName(String fileName) {
        addField(FILE_NAME, fileName);
    }

	/**
	 * The location of the file that is the object of the event, in terms of
	 * local file and directory structure.
	 */
	public static String FILE_PATH = "file_path";
    public void setFilePath(String filePath) {
        addField(FILE_PATH, filePath);
    }

	/**
	 * Access controls associated with the file affected by the event.
	 */
	public static String FILE_PERMISSION = "file_permission";
    public void setFilePermission(String filePermission) {
        addField(FILE_PERMISSION, filePermission);
    }

	/**
	 * The size of the file that is the object of the event. Indicate whether
	 * Bytes, KB, MB, GB.
	 */
	public static String FILE_SIZE = "file_size";
    public void setFileSize(long fileSize) {
        addField(FILE_SIZE, fileSize);
    }

	// ----------------------------------
	// Intrusion detection
	// ----------------------------------

	/**
	 * The category of the triggered signature.
	 */
	public static String INTRUSION_DETECTION_CATEGORY = "category";
    public void setIntrusionDetectionCategory(String intrusionDetectionCategory) {
        addField(INTRUSION_DETECTION_CATEGORY, intrusionDetectionCategory);
    }

	/**
	 * The destination of the attack detected by the intrusion detection system
	 * (IDS). If your field is named dest_host, dest_ip, dest_ipv6, or
	 * dest_nt_host you can alias it as dest to make it CIM-compliant.
	 */
	public static String INTRUSION_DETECTION_DEST = "dest";
    public void setIntrusionDetectionDest(String intrusionDetectionDest) {
        addField(INTRUSION_DETECTION_DEST, intrusionDetectionDest);
    }

	/**
	 * The device that detected the intrusion event.
	 */
	public static String INTRUSION_DETECTION_DVC = "dvc";
    public void setIntrusionDetectionDvc(String intrusionDetectionDvc) {
        addField(INTRUSION_DETECTION_DVC, intrusionDetectionDvc);
    }

	/**
	 * The type of IDS that generated the event.
	 */
	public static String INTRUSION_DETECTION_IDS_TYPE = "ids_type";
    public void setIntrusionDetectionIdsType(String intrusionDetectionIdsType) {
        addField(INTRUSION_DETECTION_IDS_TYPE, intrusionDetectionIdsType);
    }

	/**
	 * The product name of the vendor technology generating network protection
	 * data, such as IDP, Providentia, and ASA.
	 * 
	 * Note: Required for all events dealing with network protection (Change
	 * analysis, proxy, malware, intrusion detection, packet filtering, and
	 * vulnerability).
	 */
	public static String INTRUSION_DETECTION_PRODUCT = "product";
    public void setIntrusionDetectionProduct(String intrusionDetectionProduct) {
        addField(INTRUSION_DETECTION_PRODUCT, intrusionDetectionProduct);
    }

	/**
	 * The severity of the network protection event (such as critical, high,
	 * medium, low, or informational).
	 * 
	 * Note: This field is a string. Please use a severity_id field for severity
	 * ID fields that are integer data types.
	 */
	public static String INTRUSION_DETECTION_SEVERITY = "severity";
    public void setIntrusionDetectionSeverity(String intrusionDetectionSeverity) {
        addField(INTRUSION_DETECTION_SEVERITY, intrusionDetectionSeverity);
    }

	/**
	 * The name of the intrusion detected on the client (the src), such as
	 * PlugAndPlay_BO and JavaScript_Obfuscation_Fre.
	 */
	public static String INTRUSION_DETECTION_SIGNATURE = "signature";
    public void setIntrusionDetectionSignature(
            String intrusionDetectionSignature) {
        addField(INTRUSION_DETECTION_SIGNATURE, intrusionDetectionSignature);
    }

	/**
	 * The source involved in the attack detected by the IDS. If your field is
	 * named src_host, src_ip, src_ipv6, or src_nt_host you can alias it as src
	 * to make it CIM-compliant.
	 */
	public static String INTRUSION_DETECTION_SRC = "src";
	public void setIntrusionDetectionSrc(String intrusionDetectionSrc) {
		addField(INTRUSION_DETECTION_SRC, intrusionDetectionSrc);
	}

	/**
	 * The user involved with the intrusion detection event.
	 */
	public static String INTRUSION_DETECTION_USER = "user";
	public void setIntrusionDetectionUser(String intrusionDetectionUser) {
		addField(INTRUSION_DETECTION_USER, intrusionDetectionUser);
	}

	/**
	 * The vendor technology used to generate network protection data, such as
	 * IDP, Providentia, and ASA.
	 * 
	 * Note: Required for all events dealing with network protection (Change
	 * analysis, proxy, malware, intrusion detection, packet filtering, and
	 * vulnerability).
	 */
	public static String INTRUSION_DETECTION_VENDOR = "vendor";
	public void setIntrusionDetectionVendor(String intrusionDetectionVendor) {
		addField(INTRUSION_DETECTION_VENDOR, intrusionDetectionVendor);
	}


	// ----------------------------------
	// Malware - Endpoint protection
	// ----------------------------------

	/**
	 * The outcome of the infection
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_ACTION = "action";
	public void setMalwareEndpointProtectionAction(
			String malwareEndpointProtectionAction) {
		addField(MALWARE_ENDPOINT_PROTECTION_ACTION,
				malwareEndpointProtectionAction);
	}

	/**
	 * The NT domain of the destination (the dest_bestmatch).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_DEST_NT_DOMAIN = "dest_nt_domain";
	public void setMalwareEndpointProtectionDestNtDomain(
			String malwareEndpointProtectionDestNtDomain) {
		addField(MALWARE_ENDPOINT_PROTECTION_DEST_NT_DOMAIN,
				malwareEndpointProtectionDestNtDomain);
	}

	/**
	 * The cryptographic hash of the file associated with the malware event
	 * (such as the malicious or infected file).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_FILE_HASH = "file_hash";
	public void setMalwareEndpointProtectionFileHash(
			String malwareEndpointProtectionFileHash) {
		addField(MALWARE_ENDPOINT_PROTECTION_FILE_HASH,
				malwareEndpointProtectionFileHash);
	}

	/**
	 * The name of the file involved in the malware event (such as the infected
	 * or malicious file).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_FILE_NAME = "file_name";
	public void setMalwareEndpointProtectionFileName(
			String malwareEndpointProtectionFileName) {
		addField(MALWARE_ENDPOINT_PROTECTION_FILE_NAME,
				malwareEndpointProtectionFileName);
	}

	/**
	 * The path of the file involved in the malware event (such as the infected
	 * or malicious file).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_FILE_PATH = "file_path";
	public void setMalwareEndpointProtectionFilePath(
			String malwareEndpointProtectionFilePath) {
		addField(MALWARE_ENDPOINT_PROTECTION_FILE_PATH,
				malwareEndpointProtectionFilePath);
	}

	/**
	 * The product name of the vendor technology (the vendor field) that is
	 * generating malware data (such as Antivirus or EPO).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_PRODUCT = "product";
	public void setMalwareEndpointProtectionProduct(
			String malwareEndpointProtectionProduct) {
		addField(MALWARE_ENDPOINT_PROTECTION_PRODUCT,
				malwareEndpointProtectionProduct);
	}

	/**
	 * The product version number of the vendor technology installed on the
	 * client (such as 10.4.3 or 11.0.2).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_PRODUCT_VERSION = "product_version";
	public void setMalwareEndpointProtectionProductVersion(
			String malwareEndpointProtectionProductVersion) {
		addField(MALWARE_ENDPOINT_PROTECTION_PRODUCT_VERSION,
				malwareEndpointProtectionProductVersion);
	}

	/**
	 * The name of the malware infection detected on the client (the src), such
	 * as Trojan.Vundo,Spyware.Gaobot,W32.Nimbda).
	 * 
	 * Note: This field is a string. Please use a signature_id field for
	 * signature ID fields that are integer data types.
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_SIGNATURE = "signature";
	public void setMalwareEndpointProtectionSignature(
			String malwareEndpointProtectionSignature) {
		addField(MALWARE_ENDPOINT_PROTECTION_SIGNATURE,
				malwareEndpointProtectionSignature);
	}

	/**
	 * The current signature definition set running on the client, such as
	 * 11hsvx)
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_SIGNATURE_VERSION = "signature_version";
	public void setMalwareEndpointProtectionSignatureVersion(
			String malwareEndpointProtectionSignatureVersion) {
		addField(MALWARE_ENDPOINT_PROTECTION_SIGNATURE_VERSION,
				malwareEndpointProtectionSignatureVersion);
	}

	/**
	 * The target affected or infected by the malware. If your field is named
	 * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
	 * to make it CIM-compliant.
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_DEST = "dest";
	public void setMalwareEndpointProtectionDest(
			String malwareEndpointProtectionDest) {
		addField(MALWARE_ENDPOINT_PROTECTION_DEST, malwareEndpointProtectionDest);
	}

	/**
	 * The NT domain of the source (the src).
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_SRC_NT_DOMAIN = "src_nt_domain";
	public void setMalwareEndpointProtectionSrcNtDomain(
			String malwareEndpointProtectionSrcNtDomain) {
		addField(MALWARE_ENDPOINT_PROTECTION_SRC_NT_DOMAIN,
				malwareEndpointProtectionSrcNtDomain);
	}

	/**
	 * The name of the user involved in the malware event.
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_USER = "user";
	public void setMalwareEndpointProtectionUser(
			String malwareEndpointProtectionUser) {
		addField(MALWARE_ENDPOINT_PROTECTION_USER, malwareEndpointProtectionUser);
	}

	/**
	 * The name of the vendor technology generating malware data, such as
	 * Symantec or McAfee.
	 */
	public static String MALWARE_ENDPOINT_PROTECTION_VENDOR = "vendor";
	public void setMalwareEndpointProtectionVendor(
			String malwareEndpointProtectionVendor) {
		addField(MALWARE_ENDPOINT_PROTECTION_VENDOR,
				malwareEndpointProtectionVendor);
	}

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
	public static String MALWARE_NETWORK_PROTECTION_PRODUCT = "product";
	public void setMalwareNetworkProtectionProduct(
			String malwareNetworkProtectionProduct) {
		addField(MALWARE_NETWORK_PROTECTION_PRODUCT,
				malwareNetworkProtectionProduct);
	}

	/**
	 * The severity of the network protection event (such as critical, high,
	 * medium, low, or informational).
	 * 
	 * Note: This field is a string. Please use a severity_id field for severity
	 * ID fields that are integer data types.
	 */
	public static String MALWARE_NETWORK_PROTECTION_SEVERITY = "severity";
	public void setMalwareNetworkProtectionSeverity(
			String malwareNetworkProtectionSeverity) {
		addField(MALWARE_NETWORK_PROTECTION_SEVERITY,
				malwareNetworkProtectionSeverity);
	}

	/**
	 * The vendor technology used to generate network protection data, such as
	 * IDP, Proventia, and ASA.
	 * 
	 * Note: Required for all events dealing with network protection (Change
	 * analysis, proxy, malware, intrusion detection, packet filtering, and
	 * vulnerability).
	 */
	public static String MALWARE_NETWORK_PROTECTION_VENDOR = "vendor";
	public void setMalwareNetworkProtectionVendor(
			String malwareNetworkProtectionVendor) {
		addField(MALWARE_NETWORK_PROTECTION_VENDOR,
				malwareNetworkProtectionVendor);
	}


	// ----------------------------------
	// Network traffic - ESS
	// ----------------------------------

	/**
	 * The action of the network traffic.
	 */
	public static String NETWORK_TRAFFIC_ESS_ACTION = "action";
	public void setNetworkTrafficEssAction(String networkTrafficEssAction) {
		addField(NETWORK_TRAFFIC_ESS_ACTION, networkTrafficEssAction);
	}

	/**
	 * The destination port of the network traffic.
	 */
	public static String NETWORK_TRAFFIC_ESS_DEST_PORT = "dest_port";
	public void setNetworkTrafficEssDestPort(int networkTrafficEssDestPort) {
		addField(NETWORK_TRAFFIC_ESS_DEST_PORT, networkTrafficEssDestPort);
	}

	/**
	 * The product name of the vendor technology generating NetworkProtection
	 * data, such as IDP, Proventia, and ASA.
	 * 
	 * Note: Required for all events dealing with network protection (Change
	 * analysis, proxy, malware, intrusion detection, packet filtering, and
	 * vulnerability).
	 */
	public static String NETWORK_TRAFFIC_ESS_PRODUCT = "product";
	public void setNetworkTrafficEssProduct(String networkTrafficEssProduct) {
		addField(NETWORK_TRAFFIC_ESS_PRODUCT, networkTrafficEssProduct);
	}

	/**
	 * The source port of the network traffic.
	 */
	public static String NETWORK_TRAFFIC_ESS_SRC_PORT = "src_port";
	public void setNetworkTrafficEssSrcPort(int networkTrafficEssSrcPort) {
		addField(NETWORK_TRAFFIC_ESS_SRC_PORT, networkTrafficEssSrcPort);
	}

	/**
	 * The vendor technology used to generate NetworkProtection data, such as
	 * IDP, Proventia, and ASA.
	 * 
	 * Note: Required for all events dealing with network protection (Change
	 * analysis, proxy, malware, intrusion detection, packet filtering, and
	 * vulnerability).
	 */
	public static String NETWORK_TRAFFIC_ESS_VENDOR = "vendor";
	public void setNetworkTrafficEssVendor(String networkTrafficEssVendor) {
		addField(NETWORK_TRAFFIC_ESS_VENDOR, networkTrafficEssVendor);
	}

	// ----------------------------------
	// Network traffic - Generic
	// ----------------------------------

	/**
	 * The ISO layer 7 (application layer) protocol, such as HTTP, HTTPS, SSH,
	 * and IMAP.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_APP_LAYER = "app_layer";
	public void setNetworkTrafficGenericAppLayer(
			String networkTrafficGenericAppLayer) {
		addField(NETWORK_TRAFFIC_GENERIC_APP_LAYER,
				networkTrafficGenericAppLayer);
	}
	/**
	 * How many bytes this device/interface received.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_BYTES_IN = "bytes_in";
	public void setNetworkTrafficGenericBytesIn(
			long networkTrafficGenericBytesIn) {
		addField(NETWORK_TRAFFIC_GENERIC_BYTES_IN, networkTrafficGenericBytesIn);
	}


	/**
	 * How many bytes this device/interface transmitted.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_BYTES_OUT = "bytes_out";
	public void setNetworkTrafficGenericBytesOut(
			long networkTrafficGenericBytesOut) {
		addField(NETWORK_TRAFFIC_GENERIC_BYTES_OUT,
				networkTrafficGenericBytesOut);
	}

	/**
	 * 802.11 channel number used by a wireless network.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_CHANNEL = "channel";
	public void setNetworkTrafficGenericChannel(
			String networkTrafficGenericChannel) {
		addField(NETWORK_TRAFFIC_GENERIC_CHANNEL, networkTrafficGenericChannel);
	}

	/**
	 * The Common Vulnerabilities and Exposures (CVE) reference value.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_CVE = "cve";
	public void setNetworkTrafficGenericCve(String networkTrafficGenericCve) {
		addField(NETWORK_TRAFFIC_GENERIC_CVE, networkTrafficGenericCve);
	}

	/**
	 * The destination application being targeted.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_APP = "dest_app";
	public void setNetworkTrafficGenericDestApp(
			String networkTrafficGenericDestApp) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_APP, networkTrafficGenericDestApp);
	}

	/**
	 * The destination command and control service channel.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_CHANNEL = "dest_cnc_channel";
	public void setNetworkTrafficGenericDestCncChannel(
			String networkTrafficGenericDestCncChannel) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_CHANNEL,
				networkTrafficGenericDestCncChannel);
	}

	/**
	 * The destination command and control service name.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_NAME = "dest_cnc_name";
	public void setNetworkTrafficGenericDestCncName(
			String networkTrafficGenericDestCncName) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_NAME,
				networkTrafficGenericDestCncName);
	}

	/**
	 * The destination command and control service port.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_CNC_PORT = "dest_cnc_port";
	public void setNetworkTrafficGenericDestCncPort(
			String networkTrafficGenericDestCncPort) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_CNC_PORT,
				networkTrafficGenericDestCncPort);
	}

	/**
	 * The country associated with a packet's recipient.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_COUNTRY = "dest_country";
	public void setNetworkTrafficGenericDestCountry(
			String networkTrafficGenericDestCountry) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_COUNTRY,
				networkTrafficGenericDestCountry);
	}

	/**
	 * The fully qualified host name of a packet's recipient. For HTTP sessions,
	 * this is the host header.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_HOST = "dest_host";
	public void setNetworkTrafficGenericDestHost(
			String networkTrafficGenericDestHost) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_HOST,
				networkTrafficGenericDestHost);
	}

	/**
	 * The interface that is listening remotely or receiving packets locally.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_INT = "dest_int";
	public void setNetworkTrafficGenericDestInt(
			String networkTrafficGenericDestInt) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_INT, networkTrafficGenericDestInt);
	}

	/**
	 * The IPv4 address of a packet's recipient.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_IP = "dest_ip";
	public void setNetworkTrafficGenericDestIp(
			String networkTrafficGenericDestIp) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_IP, networkTrafficGenericDestIp);
	}

	/**
	 * The IPv6 address of a packet's recipient.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_IPV6 = "dest_ipv6";
	public void setNetworkTrafficGenericDestIpv6(
			String networkTrafficGenericDestIpv6) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_IPV6,
				networkTrafficGenericDestIpv6);
	}

	/**
	 * The (physical) latitude of a packet's destination.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_LAT = "dest_lat";
	public void setNetworkTrafficGenericDestLat(int networkTrafficGenericDestLat) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_LAT, networkTrafficGenericDestLat);
	}

	/**
	 * The (physical) longitude of a packet's destination.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_LONG = "dest_long";
	public void setNetworkTrafficGenericDestLong(
			int networkTrafficGenericDestLong) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_LONG,
				networkTrafficGenericDestLong);
	}

	/**
	 * The destination TCP/IP layer 2 Media Access Control (MAC) address of a
	 * packet's destination.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_MAC = "dest_mac";
	public void setNetworkTrafficGenericDestMac(
			String networkTrafficGenericDestMac) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_MAC, networkTrafficGenericDestMac);
	}

	/**
	 * The Windows NT domain containing a packet's destination.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_NT_DOMAIN = "dest_nt_domain";
	public void setNetworkTrafficGenericDestNtDomain(
			String networkTrafficGenericDestNtDomain) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_NT_DOMAIN,
				networkTrafficGenericDestNtDomain);
	}

	/**
	 * The Windows NT host name of a packet's destination.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_NT_HOST = "dest_nt_host";
	public void setNetworkTrafficGenericDestNtHost(
			String networkTrafficGenericDestNtHost) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_NT_HOST,
				networkTrafficGenericDestNtHost);
	}

	/**
	 * TCP/IP port to which a packet is being sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_PORT = "dest_port";
	public void setNetworkTrafficGenericDestPort(
			int networkTrafficGenericDestPort) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_PORT,
				networkTrafficGenericDestPort);
	}

	/**
	 * The NATed IPv4 address to which a packet has been sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_IP = "dest_translated_ip";
	public void setNetworkTrafficGenericDestTranslatedIp(
			String networkTrafficGenericDestTranslatedIp) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_IP,
				networkTrafficGenericDestTranslatedIp);
	}

	/**
	 * The NATed port to which a packet has been sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_PORT = "dest_translated_port";
	public void setNetworkTrafficGenericDestTranslatedPort(
			int networkTrafficGenericDestTranslatedPort) {
		addField(NETWORK_TRAFFIC_GENERIC_DEST_TRANSLATED_PORT,
				networkTrafficGenericDestTranslatedPort);
	}

	/**
	 * The numbered Internet Protocol version.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_IP_VERSION = "ip_version";
	public void setNetworkTrafficGenericIpVersion(
			int networkTrafficGenericIpVersion) {
		addField(NETWORK_TRAFFIC_GENERIC_IP_VERSION,
				networkTrafficGenericIpVersion);
	}

	/**
	 * The network interface through which a packet was transmitted.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_OUTBOUND_INTERFACE = "outbound_interface";
	public void setNetworkTrafficGenericOutboundInterface(
			String networkTrafficGenericOutboundInterface) {
		addField(NETWORK_TRAFFIC_GENERIC_OUTBOUND_INTERFACE,
				networkTrafficGenericOutboundInterface);
	}

	/**
	 * How many packets this device/interface received.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_PACKETS_IN = "packets_in";
	public void setNetworkTrafficGenericPacketsIn(
			long networkTrafficGenericPacketsIn) {
		addField(NETWORK_TRAFFIC_GENERIC_PACKETS_IN,
				networkTrafficGenericPacketsIn);
	}

	/**
	 * How many packets this device/interface transmitted.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_PACKETS_OUT = "packets_out";
	public void setNetworkTrafficGenericPacketsOut(
			long networkTrafficGenericPacketsOut) {
		addField(NETWORK_TRAFFIC_GENERIC_PACKETS_OUT,
				networkTrafficGenericPacketsOut);
	}

	/**
	 * The OSI layer 3 (Network Layer) protocol, such as IPv4/IPv6, ICMP, IPsec,
	 * IGMP or RIP.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_PROTO = "proto";
	public void setNetworkTrafficGenericProto(String networkTrafficGenericProto) {
		addField(NETWORK_TRAFFIC_GENERIC_PROTO, networkTrafficGenericProto);
	}

	/**
	 * The session identifier. Multiple transactions build a session.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SESSION_ID = "session_id";
	public void setNetworkTrafficGenericSessionId(
			String networkTrafficGenericSessionId) {
		addField(NETWORK_TRAFFIC_GENERIC_SESSION_ID,
				networkTrafficGenericSessionId);
	}

	/**
	 * The 802.11 service set identifier (ssid) assigned to a wireless session.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SSID = "ssid";
	public void setNetworkTrafficGenericSsid(String networkTrafficGenericSsid) {
		addField(NETWORK_TRAFFIC_GENERIC_SSID, networkTrafficGenericSsid);
	}

	/**
	 * The country from which the packet was sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_COUNTRY = "src_country";
	public void setNetworkTrafficGenericSrcCountry(
			String networkTrafficGenericSrcCountry) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_COUNTRY,
				networkTrafficGenericSrcCountry);
	}

	/**
	 * The fully qualified host name of the system that transmitted the packet.
	 * For Web logs, this is the HTTP client.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_HOST = "src_host";
	public void setNetworkTrafficGenericSrcHost(
			String networkTrafficGenericSrcHost) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_HOST, networkTrafficGenericSrcHost);
	}

	/**
	 * The interface that is listening locally or sending packets remotely.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_INT = "src_int";
	public void setNetworkTrafficGenericSrcInt(
			String networkTrafficGenericSrcInt) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_INT, networkTrafficGenericSrcInt);
	}

	/**
	 * The IPv4 address of the packet's source. For Web logs, this is the http
	 * client.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_IP = "src_ip";
	public void setNetworkTrafficGenericSrcIp(String networkTrafficGenericSrcIp) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_IP, networkTrafficGenericSrcIp);
	}

	/**
	 * The IPv6 address of the packet's source.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_IPV6 = "src_ipv6";
	public void setNetworkTrafficGenericSrcIpv6(
			String networkTrafficGenericSrcIpv6) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_IPV6, networkTrafficGenericSrcIpv6);
	}

	/**
	 * The (physical) latitude of the packet's source.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_LAT = "src_lat";
	public void setNetworkTrafficGenericSrcLat(int networkTrafficGenericSrcLat) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_LAT, networkTrafficGenericSrcLat);
	}

	/**
	 * The (physical) longitude of the packet's source.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_LONG = "src_long";
	public void setNetworkTrafficGenericSrcLong(int networkTrafficGenericSrcLong) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_LONG, networkTrafficGenericSrcLong);
	}

	/**
	 * The Media Access Control (MAC) address from which a packet was
	 * transmitted.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_MAC = "src_mac";
	public void setNetworkTrafficGenericSrcMac(
			String networkTrafficGenericSrcMac) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_MAC, networkTrafficGenericSrcMac);
	}

	/**
	 * The Windows NT domain containing the machines that generated the event.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_NT_DOMAIN = "src_nt_domain";
	public void setNetworkTrafficGenericSrcNtDomain(
			String networkTrafficGenericSrcNtDomain) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_NT_DOMAIN,
				networkTrafficGenericSrcNtDomain);
	}

	/**
	 * The Windows NT hostname of the system that generated the event.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_NT_HOST = "src_nt_host";
	public void setNetworkTrafficGenericSrcNtHost(
			String networkTrafficGenericSrcNtHost) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_NT_HOST,
				networkTrafficGenericSrcNtHost);
	}

	/**
	 * The network port from which a packet originated.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_PORT = "src_port";
	public void setNetworkTrafficGenericSrcPort(int networkTrafficGenericSrcPort) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_PORT, networkTrafficGenericSrcPort);
	}

	/**
	 * The NATed IPv4 address from which a packet has been sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_IP = "src_translated_ip";
	public void setNetworkTrafficGenericSrcTranslatedIp(
			String networkTrafficGenericSrcTranslatedIp) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_IP,
				networkTrafficGenericSrcTranslatedIp);
	}

	/**
	 * The NATed network port from which a packet has been sent.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_PORT = "src_translated_port";
	public void setNetworkTrafficGenericSrcTranslatedPort(
			int networkTrafficGenericSrcTranslatedPort) {
		addField(NETWORK_TRAFFIC_GENERIC_SRC_TRANSLATED_PORT,
				networkTrafficGenericSrcTranslatedPort);
	}

	/**
	 * The application, process, or OS subsystem that generated the event.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SYSLOG_ID = "syslog_id";
	public void setNetworkTrafficGenericSyslogId(
			String networkTrafficGenericSyslogId) {
		addField(NETWORK_TRAFFIC_GENERIC_SYSLOG_ID,
				networkTrafficGenericSyslogId);
	}

	/**
	 * The criticality of an event, as recorded by UNIX syslog.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_SYSLOG_PRIORITY = "syslog_priority";
	public void setNetworkTrafficGenericSyslogPriority(
			String networkTrafficGenericSyslogPriority) {
		addField(NETWORK_TRAFFIC_GENERIC_SYSLOG_PRIORITY,
				networkTrafficGenericSyslogPriority);
	}

	/**
	 * The TCP flag(s) specified in the event.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_TCP_FLAG = "tcp_flag";
	public void setNetworkTrafficGenericTcpFlag(
			String networkTrafficGenericTcpFlag) {
		addField(NETWORK_TRAFFIC_GENERIC_TCP_FLAG, networkTrafficGenericTcpFlag);
	}

	/**
	 * The hex bit that specifies TCP 'type of service'
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Type_of_Service">Type of
	 *      Service</a>
	 */
	public static String NETWORK_TRAFFIC_GENERIC_TOS = "tos";
	public void setNetworkTrafficGenericTos(String networkTrafficGenericTos) {
		addField(NETWORK_TRAFFIC_GENERIC_TOS, networkTrafficGenericTos);
	}

	/**
	 * The transport protocol.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_TRANSPORT = "transport";
	public void setNetworkTrafficGenericTransport(
			String networkTrafficGenericTransport) {
		addField(NETWORK_TRAFFIC_GENERIC_TRANSPORT,
				networkTrafficGenericTransport);
	}

	/**
	 * The "time to live" of a packet or datagram.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_TTL = "ttl";
	public void setNetworkTrafficGenericTtl(int networkTrafficGenericTtl) {
		addField(NETWORK_TRAFFIC_GENERIC_TTL, networkTrafficGenericTtl);
	}

	/**
	 * The numeric identifier assigned to the virtual local area network (VLAN)
	 * specified in the record.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_VLAN_ID = "vlan_id";
	public void setNetworkTrafficGenericVlanId(long networkTrafficGenericVlanId) {
		addField(NETWORK_TRAFFIC_GENERIC_VLAN_ID, networkTrafficGenericVlanId);
	}

	/**
	 * The name assigned to the virtual local area network (VLAN) specified in
	 * the record.
	 */
	public static String NETWORK_TRAFFIC_GENERIC_VLAN_NAME = "vlan_name";
	public void setNetworkTrafficGenericVlanName(
			String networkTrafficGenericVlanName) {
		addField(NETWORK_TRAFFIC_GENERIC_VLAN_NAME,
				networkTrafficGenericVlanName);
	}


	// ----------------------------------
	// Packet filtering
	// ----------------------------------

	/**
	 * The action the filtering device (the dvc_bestmatch field) performed on
	 * the communication.
	 */
	public static String PACKET_FILTERING_ACTION = "action";
	public void setPacketFilteringAction(String packetFilteringAction) {
		addField(PACKET_FILTERING_ACTION, packetFilteringAction);
	}

	/**
	 * The IP port of the packet's destination, such as 22.
	 */
	public static String PACKET_FILTERING_DEST_PORT = "dest_port";
	public void setPacketFilteringDestPort(int packetFilteringDestPort) {
		addField(PACKET_FILTERING_DEST_PORT, packetFilteringDestPort);
	}

	/**
	 * The direction the packet is traveling.
	 */
	public static String PACKET_FILTERING_DIRECTION = "direction";
	public void setPacketFilteringDirection(String packetFilteringDirection) {
		addField(PACKET_FILTERING_DIRECTION, packetFilteringDirection);
	}

	/**
	 * The name of the packet filtering device. If your field is named dvc_host,
	 * dvc_ip, or dvc_nt_host you can alias it as dvc to make it CIM-compliant.
	 */
	public static String PACKET_FILTERING_DVC = "dvc";
	public void setPacketFilteringDvc(String packetFilteringDvc) {
		addField(PACKET_FILTERING_DVC, packetFilteringDvc);
	}

	/**
	 * The rule which took action on the packet, such as 143.
	 */
	public static String PACKET_FILTERING_RULE = "rule";
	public void setPacketFilteringRule(String packetFilteringRule) {
		addField(PACKET_FILTERING_RULE, packetFilteringRule);
	}

	/**
	 * The IP port of the packet's source, such as 34541.
	 */
	public static String PACKET_FILTERING_SVC_PORT = "svc_port";
	public void setPacketFilteringSvcPort(int packetFilteringSvcPort) {
		addField(PACKET_FILTERING_SVC_PORT, packetFilteringSvcPort);
	}


	// ----------------------------------
	// Proxy
	// ----------------------------------

	/**
	 * The action taken by the proxy.
	 */
	public static String PROXY_ACTION = "action";
	public void setProxyAction(String proxyAction) {
		addField(PROXY_ACTION, proxyAction);
	}

	/**
	 * The destination of the network traffic (the remote host).
	 */
	public static String PROXY_DEST = "dest";
	public void setProxyDest(String proxyDest) {
		addField(PROXY_DEST, proxyDest);
	}

	/**
	 * The content-type of the requested HTTP resource.
	 */
	public static String PROXY_HTTP_CONTENT_TYPE = "http_content_type";
	public void setProxyHttpContentType(String proxyHttpContentType) {
		addField(PROXY_HTTP_CONTENT_TYPE, proxyHttpContentType);
	}

	/**
	 * The HTTP method used to request the resource.
	 */
	public static String PROXY_HTTP_METHOD = "http_method";
	public void setProxyHttpMethod(String proxyHttpMethod) {
		addField(PROXY_HTTP_METHOD, proxyHttpMethod);
	}

	/**
	 * The HTTP referrer used to request the HTTP resource.
	 */
	public static String PROXY_HTTP_REFER = "http_refer";
	public void setProxyHttpRefer(String proxyHttpRefer) {
		addField(PROXY_HTTP_REFER, proxyHttpRefer);
	}

	/**
	 * The HTTP response code.
	 */
	public static String PROXY_HTTP_RESPONSE = "http_response";
	public void setProxyHttpResponse(int proxyHttpResponse) {
		addField(PROXY_HTTP_RESPONSE, proxyHttpResponse);
	}

	/**
	 * The user agent used to request the HTTP resource.
	 */
	public static String PROXY_HTTP_USER_AGENT = "http_user_agent";
	public void setProxyHttpUserAgent(String proxyHttpUserAgent) {
		addField(PROXY_HTTP_USER_AGENT, proxyHttpUserAgent);
	}

	/**
	 * The product name of the vendor technology generating Network Protection
	 * data, such as IDP, Providentia, and ASA.
	 */
	public static String PROXY_PRODUCT = "product";
	public void setProxyProduct(String proxyProduct) {
		addField(PROXY_PRODUCT, proxyProduct);
	}

	/**
	 * The source of the network traffic (the client requesting the connection).
	 */
	public static String PROXY_SRC = "src";
	public void setProxySrc(String proxySrc) {
		addField(PROXY_SRC, proxySrc);
	}

	/**
	 * The HTTP response code indicating the status of the proxy request.
	 */
	public static String PROXY_STATUS = "status";
	public void setProxyStatus(int proxyStatus) {
		addField(PROXY_STATUS, proxyStatus);
	}

	/**
	 * The user that requested the HTTP resource.
	 */
	public static String PROXY_USER = "user";
	public void setProxyUser(String proxyUser) {
		addField(PROXY_USER, proxyUser);
	}

	/**
	 * The URL of the requested HTTP resource.
	 */
	public static String PROXY_URL = "url";
	public void setProxyUrl(String proxyUrl) {
		addField(PROXY_URL, proxyUrl);
	}

	/**
	 * The vendor technology generating Network Protection data, such as IDP,
	 * Providentia, and ASA.
	 */
	public static String PROXY_VENDOR = "vendor";
	public void setProxyVendor(String proxyVendor) {
		addField(PROXY_VENDOR, proxyVendor);
	}


	// ----------------------------------
	// System center
	// ----------------------------------

	/**
	 * The running application or service on the system (the src field), such as
	 * explorer.exe or sshd.
	 */
	public static String SYSTEM_CENTER_APP = "app";
	public void setSystemCenterApp(String systemCenterApp) {
		addField(SYSTEM_CENTER_APP, systemCenterApp);
	}

	/**
	 * The amount of disk space available per drive or mount (the mount field)
	 * on the system (the src field).
	 */
	public static String SYSTEM_CENTER_FREEMBYTES = "FreeMBytes";
	public void setSystemCenterFreembytes(long systemCenterFreembytes) {
		addField(SYSTEM_CENTER_FREEMBYTES, systemCenterFreembytes);
	}

	/**
	 * The version of operating system installed on the host (the src field),
	 * such as 6.0.1.4 or 2.6.27.30-170.2.82.fc10.x86_64.
	 */
	public static String SYSTEM_CENTER_KERNEL_RELEASE = "kernel_release";
	public void setSystemCenterKernelRelease(String systemCenterKernelRelease) {
		addField(SYSTEM_CENTER_KERNEL_RELEASE, systemCenterKernelRelease);
	}

	/**
	 * Human-readable version of the SystemUptime value.
	 */
	public static String SYSTEM_CENTER_LABEL = "label";
	public void setSystemCenterLabel(String systemCenterLabel) {
		addField(SYSTEM_CENTER_LABEL, systemCenterLabel);
	}

	/**
	 * The drive or mount reporting available disk space (the FreeMBytes field)
	 * on the system (the src field).
	 */
	public static String SYSTEM_CENTER_MOUNT = "mount";
	public void setSystemCenterMount(String systemCenterMount) {
		addField(SYSTEM_CENTER_MOUNT, systemCenterMount);
	}

	/**
	 * The name of the operating system installed on the host (the src), such as
	 * Microsoft Windows Server 2003 or GNU/Linux).
	 */
	public static String SYSTEM_CENTER_OS = "os";
	public void setSystemCenterOs(String systemCenterOs) {
		addField(SYSTEM_CENTER_OS, systemCenterOs);
	}

	/**
	 * The percentage of processor utilization.
	 */
	public static String SYSTEM_CENTER_PERCENTPROCESSORTIME = "PercentProcessorTime";
	public void setSystemCenterPercentprocessortime(
			int systemCenterPercentprocessortime) {
		addField(SYSTEM_CENTER_PERCENTPROCESSORTIME,
				systemCenterPercentprocessortime);
	}

	/**
	 * The setlocaldefs setting from the SE Linux configuration.
	 */
	public static String SYSTEM_CENTER_SETLOCALDEFS = "setlocaldefs";
	public void setSystemCenterSetlocaldefs(int systemCenterSetlocaldefs) {
		addField(SYSTEM_CENTER_SETLOCALDEFS, systemCenterSetlocaldefs);
	}

	/**
	 * Values from the SE Linux configuration file.
	 */
	public static String SYSTEM_CENTER_SELINUX = "selinux";
	public void setSystemCenterSelinux(String systemCenterSelinux) {
		addField(SYSTEM_CENTER_SELINUX, systemCenterSelinux);
	}

	/**
	 * The SE Linux type (such as targeted).
	 */
	public static String SYSTEM_CENTER_SELINUXTYPE = "selinuxtype";
	public void setSystemCenterSelinuxtype(String systemCenterSelinuxtype) {
		addField(SYSTEM_CENTER_SELINUXTYPE, systemCenterSelinuxtype);
	}

	/**
	 * The shell provided to the User Account (the user field) upon logging into
	 * the system (the src field).
	 */
	public static String SYSTEM_CENTER_SHELL = "shell";
	public void setSystemCenterShell(String systemCenterShell) {
		addField(SYSTEM_CENTER_SHELL, systemCenterShell);
	}

	/**
	 * The TCP/UDP source port on the system (the src field).
	 */
	public static String SYSTEM_CENTER_SRC_PORT = "src_port";
	public void setSystemCenterSrcPort(int systemCenterSrcPort) {
		addField(SYSTEM_CENTER_SRC_PORT, systemCenterSrcPort);
	}

	/**
	 * The sshd protocol version.
	 */
	public static String SYSTEM_CENTER_SSHD_PROTOCOL = "sshd_protocol";
	public void setSystemCenterSshdProtocol(String systemCenterSshdProtocol) {
		addField(SYSTEM_CENTER_SSHD_PROTOCOL, systemCenterSshdProtocol);
	}

	/**
	 * The start mode of the given service.
	 */
	public static String SYSTEM_CENTER_STARTMODE = "Startmode";
	public void setSystemCenterStartmode(String systemCenterStartmode) {
		addField(SYSTEM_CENTER_STARTMODE, systemCenterStartmode);
	}

	/**
	 * The number of seconds since the system (the src) has been "up."
	 */
	public static String SYSTEM_CENTER_SYSTEMUPTIME = "SystemUptime";
	public void setSystemCenterSystemuptime(long systemCenterSystemuptime) {
		addField(SYSTEM_CENTER_SYSTEMUPTIME, systemCenterSystemuptime);
	}

	/**
	 * The total amount of available memory on the system (the src).
	 */
	public static String SYSTEM_CENTER_TOTALMBYTES = "TotalMBytes";
	public void setSystemCenterTotalmbytes(long systemCenterTotalmbytes) {
		addField(SYSTEM_CENTER_TOTALMBYTES, systemCenterTotalmbytes);
	}

	/**
	 * The amount of used memory on the system (the src).
	 */
	public static String SYSTEM_CENTER_USEDMBYTES = "UsedMBytes";
	public void setSystemCenterUsedmbytes(long systemCenterUsedmbytes) {
		addField(SYSTEM_CENTER_USEDMBYTES, systemCenterUsedmbytes);
	}

	/**
	 * The User Account present on the system (the src).
	 */
	public static String SYSTEM_CENTER_USER = "user";
	public void setSystemCenterUser(String systemCenterUser) {
		addField(SYSTEM_CENTER_USER, systemCenterUser);
	}

	/**
	 * The number of updates the system (the src) is missing.
	 */
	public static String SYSTEM_CENTER_UPDATES = "updates";
	public void setSystemCenterUpdates(long systemCenterUpdates) {
		addField(SYSTEM_CENTER_UPDATES, systemCenterUpdates);
	}


	// ----------------------------------
	// Traffic
	// ----------------------------------

	/**
	 * The destination of the network traffic. If your field is named dest_host,
	 * dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest to make it
	 * CIM-compliant.
	 */
	public static String TRAFFIC_DEST = "dest";
	public void setTrafficDest(String trafficDest) {
		addField(TRAFFIC_DEST, trafficDest);
	}

	/**
	 * The name of the packet filtering device. If your field is named dvc_host,
	 * dvc_ip, or dvc_nt_host you can alias it as dvc to make it CIM-compliant.
	 */
	public static String TRAFFIC_DVC = "dvc";
	public void setTrafficDvc(String trafficDvc) {
		addField(TRAFFIC_DVC, trafficDvc);
	}

	/**
	 * The source of the network traffic. If your field is named src_host,
	 * src_ip, src_ipv6, or src_nt_host you can alias it as src to make it
	 * CIM-compliant.
	 */
	public static String TRAFFIC_SRC = "src";
	public void setTrafficSrc(String trafficSrc) {
		addField(TRAFFIC_SRC, trafficSrc);
	}


	// ----------------------------------
	// Update
	// ----------------------------------

	/**
	 * The name of the installed update.
	 */
	public static String UPDATE_PACKAGE = "package";
	public void setUpdatePackage(String updatePackage) {
		addField(UPDATE_PACKAGE, updatePackage);
	}


	// ----------------------------------
	// User information updates
	// ----------------------------------

	/**
	 * A user that has been affected by a change. For example, user fflanda
	 * changed the name of user rhallen, so affected_user=rhallen.
	 */
	public static String USER_INFO_UPDATES_AFFECTED_USER = "affected_user";
	public void setUserInfoUpdatesAffectedUser(
			String userInfoUpdatesAffectedUser) {
		addField(USER_INFO_UPDATES_AFFECTED_USER, userInfoUpdatesAffectedUser);
	}

	/**
	 * The user group affected by a change.
	 */
	public static String USER_INFO_UPDATES_AFFECTED_USER_GROUP = "affected_user_group";
	public void setUserInfoUpdatesAffectedUserGroup(
			String userInfoUpdatesAffectedUserGroup) {
		addField(USER_INFO_UPDATES_AFFECTED_USER_GROUP,
				userInfoUpdatesAffectedUserGroup);
	}

	/**
	 * The identifier of the user group affected by a change.
	 */
	public static String USER_INFO_UPDATES_AFFECTED_USER_GROUP_ID = "affected_user_group_id";
	public void setUserInfoUpdatesAffectedUserGroupId(
			int userInfoUpdatesAffectedUserGroupId) {
		addField(USER_INFO_UPDATES_AFFECTED_USER_GROUP_ID,
				userInfoUpdatesAffectedUserGroupId);
	}

	/**
	 * The identifier of the user affected by a change.
	 */
	public static String USER_INFO_UPDATES_AFFECTED_USER_ID = "affected_user_id";
	public void setUserInfoUpdatesAffectedUserId(
			int userInfoUpdatesAffectedUserId) {
		addField(USER_INFO_UPDATES_AFFECTED_USER_ID,
				userInfoUpdatesAffectedUserId);
	}

	/**
	 * The security context associated with the user affected by a change.
	 */
	public static String USER_INFO_UPDATES_AFFECTED_USER_PRIVILEGE = "affected_user_privilege";
	public void setUserInfoUpdatesAffectedUserPrivilege(
			String userInfoUpdatesAffectedUserPrivilege) {
		addField(USER_INFO_UPDATES_AFFECTED_USER_PRIVILEGE,
				userInfoUpdatesAffectedUserPrivilege);
	}

	/**
	 * The name of the user affected by the recorded event.
	 */
	public static String USER_INFO_UPDATES_USER = "user";
	public void setUserInfoUpdatesUser(String userInfoUpdatesUser) {
		addField(USER_INFO_UPDATES_USER, userInfoUpdatesUser);
	}

	/**
	 * A user group that is the object of an event, expressed in human-readable
	 * terms.
	 */
	public static String USER_INFO_UPDATES_USER_GROUP = "user_group";
	public void setUserInfoUpdatesUserGroup(String userInfoUpdatesUserGroup) {
		addField(USER_INFO_UPDATES_USER_GROUP, userInfoUpdatesUserGroup);
	}

	/**
	 * The numeric identifier assigned to the user group event object.
	 */
	public static String USER_INFO_UPDATES_USER_GROUP_ID = "user_group_id";
	public void setUserInfoUpdatesUserGroupId(int userInfoUpdatesUserGroupId) {
		addField(USER_INFO_UPDATES_USER_GROUP_ID, userInfoUpdatesUserGroupId);
	}

	/**
	 * The system-assigned identifier for the user affected by an event.
	 */
	public static String USER_INFO_UPDATES_USER_ID = "user_id";
	public void setUserInfoUpdatesUserId(int userInfoUpdatesUserId) {
		addField(USER_INFO_UPDATES_USER_ID, userInfoUpdatesUserId);
	}

	/**
	 * The security context associated with the object of an event (the affected
	 * user).
	 */
	public static String USER_INFO_UPDATES_USER_PRIVILEGE = "user_privilege";
	public void setUserInfoUpdatesUserPrivilege(
			String userInfoUpdatesUserPrivilege) {
		addField(USER_INFO_UPDATES_USER_PRIVILEGE, userInfoUpdatesUserPrivilege);
	}

	/**
	 * The name of the user that is the subject of an event--the user executing
	 * the action, in other words.
	 */
	public static String USER_INFO_UPDATES_USER_SUBJECT = "user_subject";
	public void setUserInfoUpdatesUserSubject(String userInfoUpdatesUserSubject) {
		addField(USER_INFO_UPDATES_USER_SUBJECT, userInfoUpdatesUserSubject);
	}

	/**
	 * The ID number of the user that is the subject of an event.
	 */
	public static String USER_INFO_UPDATES_USER_SUBJECT_ID = "user_subject_id";
	public void setUserInfoUpdatesUserSubjectId(int userInfoUpdatesUserSubjectId) {
		addField(USER_INFO_UPDATES_USER_SUBJECT_ID, userInfoUpdatesUserSubjectId);
	}

	/**
	 * The security context associated with the subject of an event (the user
	 * causing a change).
	 */
	public static String USER_INFO_UPDATES_USER_SUBJECT_PRIVILEGE = "user_subject_privilege";
	public void setUserInfoUpdatesUserSubjectPrivilege(
			String userInfoUpdatesUserSubjectPrivilege) {
		addField(USER_INFO_UPDATES_USER_SUBJECT_PRIVILEGE,
				userInfoUpdatesUserSubjectPrivilege);
	}


	// ----------------------------------
	// Vulnerability
	// ----------------------------------

	/**
	 * The category of the discovered vulnerability.
	 */
	public static String VULNERABILITY_CATEGORY = "category";
	public void setVulnerabilityCategory(String vulnerabilityCategory) {
		addField(VULNERABILITY_CATEGORY, vulnerabilityCategory);
	}

	/**
	 * The host with the discovered vulnerability. If your field is named
	 * dest_host, dest_ip, dest_ipv6, or dest_nt_host you can alias it as dest
	 * to make it CIM-compliant.
	 */
	public static String VULNERABILITY_DEST = "dest";
	public void setVulnerabilityDest(String vulnerabilityDest) {
		addField(VULNERABILITY_DEST, vulnerabilityDest);
	}

	/**
	 * The operating system of the host containing the vulnerability detected on
	 * the client (the src field), such as SuSE Security Update, or cups
	 * security update.
	 */
	public static String VULNERABILITY_OS = "os";
	public void setVulnerabilityOs(String vulnerabilityOs) {
		addField(VULNERABILITY_OS, vulnerabilityOs);
	}

	/**
	 * The severity of the discovered vulnerability.
	 */
	public static String VULNERABILITY_SEVERITY = "severity";
	public void setVulnerabilitySeverity(String vulnerabilitySeverity) {
		addField(VULNERABILITY_SEVERITY, vulnerabilitySeverity);
	}

	/**
	 * The name of the vulnerability detected on the client (the src field),
	 * such as SuSE Security Update, or cups security update.
	 */
	public static String VULNERABILITY_SIGNATURE = "signature";
	public void setVulnerabilitySignature(String vulnerabilitySignature) {
		addField(VULNERABILITY_SIGNATURE, vulnerabilitySignature);
	}


	// ----------------------------------
	// Windows administration
	// ----------------------------------

	/**
	 * The object name (associated only with Windows).
	 */
	public static String WINDOWS_ADMIN_OBJECT_NAME = "object_name";
	public void setWindowsAdminObjectName(String windowsAdminObjectName) {
		addField(WINDOWS_ADMIN_OBJECT_NAME, windowsAdminObjectName);
	}

	/**
	 * The object type (associated only with Windows).
	 */
	public static String WINDOWS_ADMIN_OBJECT_TYPE = "object_type";
	public void setWindowsAdminObjectType(String windowsAdminObjectType) {
		addField(WINDOWS_ADMIN_OBJECT_TYPE, windowsAdminObjectType);
	}

	/**
	 * The object handle (associated only with Windows).
	 */
	public static String WINDOWS_ADMIN_OBJECT_HANDLE = "object_handle";
	public void setWindowsAdminObjectHandle(String windowsAdminObjectHandle) {
		addField(WINDOWS_ADMIN_OBJECT_HANDLE, windowsAdminObjectHandle);
	}
}
