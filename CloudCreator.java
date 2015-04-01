// General imports
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;

// Exception handling imports
import java.net.URISyntaxException;
import java.io.IOException;
import java.security.InvalidKeyException;

import javax.xml.parsers.ParserConfigurationException;

import com.microsoft.windowsazure.exception.ServiceException;

import org.xml.sax.SAXException;
// import java.util.concurrent.ExecutionException;
// import javax.xml.transform.TransformerException;
// import java.util.ArrayList;
// import java.lang.Object;




// Imports for service management client and configuration
// import com.microsoft.windowsazure.core.*;
import com.microsoft.windowsazure.Configuration;
// import com.microsoft.windowsazure.management.configuration.*;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import com.microsoft.windowsazure.management.ManagementClient;
import com.microsoft.windowsazure.management.ManagementService;

// Service management imports for storage accounts
import com.microsoft.windowsazure.management.storage.*;
import com.microsoft.windowsazure.management.storage.models.*;
// Imports for blob/storage service, including containers
import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;
// Service management imports for compute services
import com.microsoft.windowsazure.management.compute.*;
// Imports for authentication
import com.microsoft.windowsazure.core.utils.KeyStoreType;

public class CloudCreator {

    // Instantiate management client objects
    protected static ManagementClient managementClient;
    protected static StorageManagementClient storageManagementClient;
    protected static ComputeManagementClient computeManagementClient;

    // Define configuration parameters for the service management client, and
    // create a configuration object for the various management clients to use.
    // (See createManagementClient in StorageManagementIntegrationTestBase.java.)

    public static Configuration createConfiguration() throws Exception {
        // Parameter definitions used for authentication
	    String uri = "https://management.core.windows.net/";
		String subscriptionId = "4025e99d-5de8-4da9-9555-3c31698595c3";
		String keyStoreLocation = "c:\\certificates\\jamlcert1027.jks";
		String keyStorePassword = "Poindexter";

	    // Set configuration parameters for the service management client
	    Configuration config = ManagementConfiguration.configure(
		    new URI(uri),
		    subscriptionId,
		    keyStoreLocation,  // Path to the JKS file
		    keyStorePassword,  // Password for the JKS file
		    KeyStoreType.jks   // Flag that you are using a JKS keystore
		);

	    return config;
    }


    // Define a method to create the management client.
    public static void createManagementClient() throws Exception {
    	Configuration config = createConfiguration();
    	managementClient = ManagementService.create(config);
    }


    // Define a method to create the storage management client.
    public static void createStorageManagementClient() throws Exception {
        Configuration config = createConfiguration();
        storageManagementClient = StorageManagementService.create(config);
    }


    // Define a method to create the compute client (VM).
    public static void createComputeManagementClient() throws Exception {
        Configuration config = createConfiguration();
        computeManagementClient = ComputeManagementService.create(config);
    }


    // Define a method to create the storage account for the virtual machine's image (VHD file).
    public static void createStorageAccount(String storageAccountName, String storageContainerName) throws Exception {

        // Define storage management client parameters.
        String storageAccountDescription = "This is a test storage account created by the AzureCloudDemo app.";
        String storageLocation = "West US";
        // Need the account key to construct the connection string; initialize to null then use getPrimaryKey() to retrieve the key
        // String storageAccountType = "Standard_LRS";  // Is this value needed?

        // Set parameters for the storage account.
        StorageAccountCreateParameters storageAccountParameters = new StorageAccountCreateParameters();
        storageAccountParameters.setName(storageAccountName);
        storageAccountParameters.setLabel(storageAccountDescription);
        storageAccountParameters.setLocation(storageLocation);
        // storageAccountParameters.setAccountType(storageAccountType);  // Q: The test code calls setAccountType(); do I need this?

        // Create the storage account.
        storageManagementClient.getStorageAccountsOperations().create(storageAccountParameters);
    }


    // Retrieve the storage account key for the specified storage account.
    public static String retrieveStorageAccountKey(String storageAccountName) throws Exception {
        String storageAccountKey = "";
        StorageAccountGetKeysResponse storageAccountGetKeysResponse = storageManagementClient.getStorageAccountsOperations().getKeys(storageAccountName);
        storageAccountKey = storageAccountGetKeysResponse.getPrimaryKey();
        return storageAccountKey;
    }


    // Define a method to create a storage container.
    public static void createStorageContainer(String storageAccountName, String storageContainerName) throws Exception {

    	try {
            // Retrieve the storage account key, which is needed for the connection string.
    		String storageAccountKey = retrieveStorageAccountKey(storageAccountName);

            // Define a connection string using the account name and account key.
            String storageConnectionString =
                "DefaultEndpointsProtocol=http;" +
                "AccountName=" + storageAccountName + ";" +
                "AccountKey=" + storageAccountKey;

            // Retrieve the storage account using the connection string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

            // Create the blob client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Get a reference to a container.
            CloudBlobContainer blobContainer = blobClient.getContainerReference(storageContainerName);

            // Create the blob container if one does not exist.
            blobContainer.createIfNotExists();

            // return blobContainer;
            }

    	catch (Exception e)
    	{
    	    // Output the stack trace.
    	    e.printStackTrace();

    	    // return null;
    	}
    }


    // Define a method to upload the VHD file to the blob container.
    protected static void uploadFileToContainer(String storageAccountName, String storageContainerName,
    		String vhdFilePath, String blobFileName)
        throws Exception, InvalidKeyException, URISyntaxException, StorageException, InterruptedException, IOException {

    	try {
            // Retrieve the storage account key, which is needed for the connection string.
    		String storageAccountKey = retrieveStorageAccountKey(storageAccountName);

            // Define a connection string for the storage account, using the account name and account key.
            String storageConnectionString =
                "DefaultEndpointsProtocol=http;" +
                "AccountName=" + storageAccountName + ";" +
                "AccountKey=" + storageAccountKey;

        	// Retrieve the storage account with the connection string.
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

            // Create the blob client.
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

            // Get a reference to the container previously created by createStorageContainer().
            CloudBlobContainer container = blobClient.getContainerReference(storageContainerName);

            // Upload a local VHD file to the blob container. (The file must be VHD format.)
            CloudBlockBlob blockBlob = container.getBlockBlobReference(blobFileName);
            File fileSource = new File(vhdFilePath + blobFileName);
            blockBlob.upload(new FileInputStream(fileSource), fileSource.length());
    	}

    	catch (Exception e) {
    	    // Output the stack trace.
    	    e.printStackTrace();
    	}


    }


    // ----- Begin VM creation code -----
    /*

    private static ArrayList<Role> createRoleList(String hostedServiceName, String storageAccountName, String storageContainer) throws Exception {
        ArrayList<Role> roleList = new ArrayList<Role>();
        Role role = new Role();
        String roleName = "VM-0120";
        String computerName = "VM-0120";
        String adminUserPassword = "your-password";
        String adminUserName = "your-user-name";
        URI mediaLinkUriValue =  new URI("http://"+ storageAccountName + ".blob.core.windows.net/" + storageContainer + "/" + "VM-0120" + ".vhd");
        String osVHarddiskName = "VM-0120" + "oshdname";
        String operatingSystemName ="Windows";

        ArrayList<ConfigurationSet> configurationSetList = new ArrayList<ConfigurationSet>();
        ConfigurationSet configurationSet = new ConfigurationSet();
        configurationSet.setConfigurationSetType(ConfigurationSetTypes.WINDOWSPROVISIONINGCONFIGURATION);
        configurationSet.setComputerName(computerName);
        configurationSet.setAdminPassword(adminUserPassword);
        configurationSet.setAdminUserName(adminUserName);
        configurationSet.setEnableAutomaticUpdates(false);
        configurationSet.setHostName(hostedServiceName + ".cloudapp.net");
        configurationSetList.add(configurationSet);

        String sourceImageName = getOSSourceImage();
        OSVirtualHardDisk oSVirtualHardDisk = new OSVirtualHardDisk();
        oSVirtualHardDisk.setName(osVHarddiskName);
        oSVirtualHardDisk.setHostCaching(VirtualHardDiskHostCaching.READWRITE);
        oSVirtualHardDisk.setOperatingSystem(operatingSystemName);
        oSVirtualHardDisk.setMediaLink(mediaLinkUriValue);
        oSVirtualHardDisk.setSourceImageName(sourceImageName);

        role.setRoleName(roleName);
        role.setRoleType(VirtualMachineRoleType.PersistentVMRole.toString());
        role.setRoleSize(VirtualMachineRoleSize.MEDIUM);
        role.setProvisionGuestAgent(true);
        role.setConfigurationSets(configurationSetList);
        role.setOSVirtualHardDisk(oSVirtualHardDisk);
        roleList.add(role);
        return roleList;
    }


    // Retrieve the OS image
    private static String getOSSourceImage() throws Exception {
        String sourceImageName = null;
        VirtualMachineOSImageListResponse virtualMachineImageListResponse = computeManagementClient.getVirtualMachineOSImagesOperations().list();
        ArrayList<VirtualMachineOSImageListResponse.VirtualMachineOSImage>
        virtualMachineOSImagelist = virtualMachineImageListResponse.getImages();
        return sourceImageName;
    }


    // Create the OS virtual hard disk
    private OSVirtualHardDisk createOSVirtualHardDisk(String osVHarddiskName, String
    		operatingSystemName, URI mediaLinkUriValue, String sourceImageName) {
        OSVirtualHardDisk oSVirtualHardDisk = new OSVirtualHardDisk();
        oSVirtualHardDisk.setName(osVHarddiskName);
        oSVirtualHardDisk.setHostCaching(VirtualHardDiskHostCaching.READWRITE);
        oSVirtualHardDisk.setOperatingSystem(operatingSystemName);
        oSVirtualHardDisk.setMediaLink(mediaLinkUriValue);
        oSVirtualHardDisk.setSourceImageName(sourceImageName);
        return oSVirtualHardDisk;
    }


    // Define parameters for the VM (VirtualMachineCreateParameters)
    private VirtualMachineCreateParameters createVirtualMachineCreateParameter(String roleName, ArrayList<ConfigurationSet> configlist, OSVirtualHardDisk oSVirtualHardDisk, String availabilitySetNameValue) {
            VirtualMachineCreateParameters createParameters = new VirtualMachineCreateParameters();
        createParameters.setRoleName(roleName);
        createParameters.setRoleSize(VirtualMachineRoleSize.MEDIUM);
        createParameters.setProvisionGuestAgent(true);
        createParameters.setConfigurationSets(configlist);
        createParameters.setOSVirtualHardDisk(oSVirtualHardDisk);
        createParameters.setAvailabilitySetName(availabilitySetNameValue);
    return createParameters;
    }


    // createConfigList()
    private ArrayList<ConfigurationSet> createConfigList(String computerName,
            String adminuserPassword, String adminUserName) {
        ArrayList<ConfigurationSet> configlist = new ArrayList<ConfigurationSet>();
        ConfigurationSet configset = new ConfigurationSet();
        configset.setConfigurationSetType(ConfigurationSetTypes.WINDOWSPROVISIONINGCONFIGURATION);
        configset.setComputerName(computerName);
        configset.setAdminPassword(adminuserPassword);
        configset.setAdminUserName(adminUserName);
        configset.setEnableAutomaticUpdates(false);
        configlist.add(configset);
        return configlist;
    }


    // Define a method to create a virtual machine
    public void createVirtualMachines(String storageAccountName, String hostedServiceName) throws Exception {
        String roleName = "vm1";
        String computerName = "vm1";
        String adminuserPassword = "!12";
        String adminUserName = "your-user-name";
        String osVHarddiskName = "oshdname";
        String operatingSystemName ="Windows";
        String deploymentName = "deployment1";
        String storageContainer = "vhd-store";
        URI mediaLinkUriValue =  new URI("http://"+ storageAccountName + ".blob.core.windows.net/" + storageContainer + "/" + ".vhd");

        ArrayList<ConfigurationSet> configlist = createConfigList(computerName, adminuserPassword, adminUserName);

        String sourceImageName = getOSSourceImage();
        OSVirtualHardDisk oSVirtualHardDisk = createOSVirtualHardDisk(osVHarddiskName, operatingSystemName, mediaLinkUriValue, sourceImageName);
        VirtualMachineCreateParameters createParameters = createVirtualMachineCreateParameter(roleName, configlist, oSVirtualHardDisk, null);

        OperationResponse operationResponse = computeManagementClient.getVirtualMachinesOperations().create(hostedServiceName, deploymentName, createParameters);
    }


    // Define a method to create a hosted cloud service
    protected static void createHostedService(String hostedServiceName) throws InterruptedException, ExecutionException, ServiceException, IOException, ParserConfigurationException, SAXException, TransformerException, URISyntaxException {

    	// Define parameter values for the hosted service
    	final String hostedServiceLabel = "hosted-service-label";
    	final String hostedServiceDescription = "This hosted service was created by AzureCloudDemo.";
    	final String vmLocation = "West US";

        // Create a hosted service, which is required for the VM deployment
        HostedServiceCreateParameters hsCreateParameters = new HostedServiceCreateParameters();

        // Set parameter values for the hosted service
        // Friendly name of the cloud service for your tracking purposes, up to 100 characters in length:
        hsCreateParameters.setLabel(hostedServiceLabel);
        // Name for the cloud service used to access the service; this is the DNS prefix name and must be unique within Azure:
        hsCreateParameters.setServiceName(hostedServiceName);
        // Optional; description of the cloud service, up to 1024 characters in length:
        hsCreateParameters.setDescription(hostedServiceDescription);
        // Optional; the location where the cloud service is created:
        hsCreateParameters.setLocation(vmLocation);

    	// Create the hosted service
        final HostedServiceOperations hostedServicesOperations;
        hostedServicesOperations = computeManagementClient.getHostedServicesOperations();
        OperationResponse hostedServiceOperationResponse = hostedServicesOperations.create(hsCreateParameters);
        System.out.println("hostedservice created: " + hostedServiceName);
    }


    // Define a method to create a VM deployment
    protected static void createVMDeployment(String hostedServiceName, String storageAccountName, String storageContainer) throws Exception {

        createHostedService(hostedServiceName);

        ArrayList<Role> deploymentRoleList = createRoleList(hostedServiceName, storageAccountName, storageContainer);

    	// Define parameter values for the VM deployment
    	final String deploymentName = "deployment-0120d";
    	final String deploymentLabel = "Deployment #0120";

    	// Set parameters for the VM deployment
        VirtualMachineCreateDeploymentParameters deploymentParameters = new VirtualMachineCreateDeploymentParameters();
        // Specifies the environment in which to deploy the virtual machine (Staging or Production):
        deploymentParameters.setDeploymentSlot(DeploymentSlot.Staging);
        // The name of the deployment, which must be unique among all deployments for the hosted service:
        deploymentParameters.setName(deploymentName);
        // Friendly name for the hosted service you can use for tracking purposes:
        deploymentParameters.setLabel(deploymentLabel);
        // Set roles, which are provisioning details for the new virtual machine deployment:
        deploymentParameters.setRoles(deploymentRoleList);

        OperationResponse operationResponse = computeManagementClient.getVirtualMachinesOperations().createDeployment(hostedServiceName, deploymentParameters);
    }

    // End VM creation code
    */


    public static void main(String[] args)
		throws IOException, URISyntaxException, ServiceException,
		ParserConfigurationException, SAXException, Exception {

    	// Create a management service client plus a management client for each cloud resource being created
    	createManagementClient();
    	createStorageManagementClient();
    	createComputeManagementClient();

        // Name the storage account that contains the virtual machine's VHD file; use only lower case for the name.
        String storageAccountName = "storage0325";
        // Name the storage container; use only lower case for the name.
        String storageContainerName = "container0325";

        // Specify the path to a local file to be uploaded to the storage container
        String vhdFilePath = "C:\\Temp\\vm-ubuntu-0325-osdisk.vhd";
        // Specify the name of the blob in which the VHD file will be stored
        String blobFileName = "vm-ubuntu-0325-osdisk.vhd";

        try {
            // Create the storage account.
            createStorageAccount(storageAccountName, storageContainerName);

        	// Create a container in the storage account.
            createStorageContainer(storageAccountName, storageContainerName);

            // Upload the VHD file to the container
        	uploadFileToContainer(storageAccountName, storageContainerName, vhdFilePath, blobFileName);

        	// Create VM
        	// createVMDeployment(hostedServiceName, storageAccountName, storageContainer);
        }

    	catch (Exception e) {
    		e.printStackTrace();
    	}

    	// createDisk();

		// Create a virtual machine
		// createVirtualMachine();

		// Create a cloud service
		// createCloudService();

    }
}
