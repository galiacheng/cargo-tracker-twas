# Run a Java EE 7 application in IBM WebSphere Application Server Traditional Network Deployment V9 on Azure VMs

The project demonstrates how you can configure and run a Java EE 7 application in IBM WebSphere Application Server Traditional (tWAS) Network Deployment V9 on Azure VMs. The project is directly based on the well-known Jakarta EE sample application Cargo Tracker V1.0. For further details on the project, please visit: https://eclipse-ee4j.github.io/cargotracker/.

# Getting Started

Environment setup：

* An Azure subscription.
* Get the project source code.
* Ensure you are running Java SE 8.
* Make sure JAVA_HOME is set.
* Make sure Maven is set up properly.
* AZ CLI, this project is tested with `2.58.0`.

Go to the project root and run `mvn clean install` to build the application. You will get:

* A WAR package in `cargotracker/target/cargo-tracker.war`.
* An EAR package in `cargotracker-was-application/target/cargo-tracker.ear`, which will be deployed to tWAS.

## Standup WebSphere Application Server (traditional) Cluster on Azure VMs

The project leverages [Azure Marketplace offer for WebSphere Application Server](https://aka.ms/websphere-on-azure-portal) to standup a tWAS cluster. Follow [Deploy WebSphere Application Server (traditional) Cluster on Azure Virtual Machines](https://learn.microsoft.com/azure/developer/java/ee/traditional-websphere-application-server-virtual-machines?tabs=basic), you will deploy a cluster with 4 members. Write down your credentials for WebSphere administrator.

After the deployment finishes, select the **Outputs** section on the left panel and write down the administrative console and IHS console URLs.

Return back to this project.

## Sign in Azure

If you haven't already, sign into your Azure subscription by using the `az login` command and follow the on-screen directions.

```
az login --use-device-code
```

If you have multiple Azure tenants associated with your Azure credentials, you must specify which tenant you want to sign in to. You can do this with the `--tenant` option. For example, `az login --tenant contoso.onmicrosoft.com`.

## Create Azure Database for PostgreSQL Flexible Server

Cargo Tracker requires a database for persistence. This project creates a PostgreSQL Flexible Server for it. 

Run the following commands to create PostgreSQL database.

```
export RESOURCE_GROUP_NAME=<the-resource-group-your-deploy-twas>
export DB_SERVER_NAME=postgresql$(date +%s)
export DB_NAME=cargotracker
export DB_ADMIN_USER_NAME=wasadmin
export DB_ADMIN_PSW=Secret123456
export LOCATION=eastus
```

Create server and database.

```
az postgres flexible-server create \
  --resource-group ${RESOURCE_GROUP_NAME} \
  --name ${DB_SERVER_NAME} \
  --location ${LOCATION} \
  --admin-user ${DB_ADMIN_USER_NAME} \
  --admin-password ${DB_ADMIN_PSW} \
  --version 16 \
  --public-access 0.0.0.0 \
  --tier Burstable \
  --sku-name Standard_B1ms \
  --yes

az postgres flexible-server db create \
  --resource-group ${RESOURCE_GROUP_NAME} \
  --server-name ${DB_SERVER_NAME} \
  --database-name ${DB_NAME}
```

Configure firewall rule.

```
echo "Allow Access To Azure Services"
az postgres flexible-server firewall-rule create \
  -g ${RESOURCE_GROUP_NAME} \
  -n ${DB_SERVER_NAME} \
  -r "AllowAllWindowsAzureIps" \
  --start-ip-address "0.0.0.0" \
  --end-ip-address "0.0.0.0"

echo "Connection string:"
echo "jdbc:postgresql://${DB_SERVER_NAME}.postgres.database.azure.com:5432/${DB_NAME}?user=${DB_ADMIN_USER_NAME}&password=${DB_ADMIN_PSW}&sslmode=require"
```

## Install PostgreSQL driver

The tWAS installation does not include the PostgreSQL driver. Follow the steps to install in manually.

Download PostgreSQL driver and save it in each managed VM.

```
export POSTGRESQL_DRIVER_URL="https://jdbc.postgresql.org/download/postgresql-42.5.1.jar"
export DRIVER_TARGET_PATH="/datadrive/IBM/WebSphere/ND/V9/postgresql"

vmList=$(az vm list --resource-group ${RESOURCE_GROUP_NAME} --query [].name -otsv | grep "managed")

for vm in ${vmList}
do 
    az vm run-command invoke \
      --resource-group $RESOURCE_GROUP_NAME \
      --name ${vm} \
      --command-id RunShellScript \
      --scripts "sudo mkdir ${DRIVER_TARGET_PATH}; sudo curl ${POSTGRESQL_DRIVER_URL} -o ${DRIVER_TARGET_PATH}/postgresql.jar"
done

echo "PostgreSQL driver path:"
echo ${DRIVER_TARGET_PATH}/postgresql.jar
```

You should find success message for each VM like:

```
{
  "value": [
    {
      "code": "ProvisioningState/succeeded",
      "displayStatus": "Provisioning succeeded",
      "level": "Info",
      "message": "Enable succeeded: \n[stdout]\n\n[stderr]\n  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current\n                                 Dload  Upload   Total   Spent    Left  Speed\n\r  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0\r100 1022k  100 1022k    0     0  1932k      0 --:--:-- --:--:-- --:--:-- 1936k\n",
      "time": null
    }
  ]
}
```

## Configure Console Preferences to synchronize changes with Nodes

First, configure the Console to synchronize changes with Nodes. The changes will be applied to all nodes once you save them.

* Open the administrative console in your Web browser and login with WebSphere administrator credentials.
* In the left navigation panel, select **System administration** -> **Console Preference**.
* Select **Synchronize changes with Nodes**.
* Select **Apply**. You will find message saying "Your preferences have been changed."

## Create data source connection in tWAS

In this section, you'll configure the data source using IBM console.

Current tWAS cluster does not ship with PostgreSQL database provider. Follow the steps create a provider:

* In the left navigation panel, select **Resources** -> **JDBC** -> **JDBC providers**.
* In the **Data source** panel, change scope with **Cluster=MyCluster**. Then select **New...** button to create a new data source.
  * In **Step 1**:
    * For **Database type**, select **User-defined**.
    * For **Implementation class name**, fill in value `org.postgresql.ds.PGConnectionPoolDataSource`.
    * For **Name**, fill in `PostgreSQLJDBCProvider`.
    * Select **Next**.
  * In **Step 2**:
    * For **Class path**, fill in `/datadrive/IBM/WebSphere/ND/V9/postgresql/postgresql.jar`, the same value with the printed "PostgreSQL driver path" previously.
    * Select **Next**.
  * In **Step 3**:
    * Select **Finish**.
* Select **Save** to save the configuration.
* To load the drive, you have to restart the cluster.
  * In the left navigation panel, select **Servers** -> **Clusters** -> **WebSphere application server clusters**.
  * Check the box next to **MyCluster**.
  * Select **Stop** to stop the cluster. 
    * Select **OK** in the **Stop cluste** page.
    * Refresh the status by clicking refresh button next to **Status** column.
    * Wait for the cluster changes to **Stop** state.
  * Check the box next to **MyCluster**.
  * Select **Start** to start the cluster. Refresh the status by clicking refresh button next to **Status** column. Do not move on before the status is in **Started** state.

Follow the steps to create data source:

* In the left navigation panel, select **Resources** -> **JDBC** -> **Data sources**.
* In the **Data source** panel, change scope with **Cluster=MyCluster**. Then select **New...** button to create a new data source.
  * In **Step 1**:
    * For **Data source name**, fill in `CargoTrackerDB`. 
    * For **JNDI name**, fill in `jdbc/CargoTrackerDB`.
    * Select **Next**.
  * In **Step 2**:
    * Select **Select an existing JDBC provider**.
    * From the drop down, select `PostgreSQLJDBCProvider`.
    * Select **Next**.
  * In **Step 3**:
    * For **Data store helper class name**, fill in `com.ibm.websphere.rsadapter.GenericDataStoreHelper`.
    * Select **Next**.
  * In **Step 4**:
    * Select **Next**.
  * In **Summary**, select **Finish**.
  * Select **Save** to save the configuration.
  * Select the data source **CargoTrackerDB** in the table, continue to configure URL.
    * Select **Custom properties**. From the table, from column **Name**, find the row whose name is **URL**. If no, select **New...** to create one.
    * Select **URL**. Fill in value with database connection string that is printed previously. 
    * Select **Apply**. 
    * Select **OK**.
    * Select **Save** to save the configuration.

Validate the data source connection:

  * Select hyperlink **CargoTrackerDB** to return back to the **Data source** panel.
  * Select **Test connection**. If the data source configuration is correct, you will find message like "The test connection operation for data source CargoTrackerDB on server nodeagent at node managed6ba334VM2Node01 was successful". If there is any error, resolve it before you move on.

## Create JMS queues

In this section, you'll create a JMS Bus, a JMS Queue Connection Factory, five JMS Queues and 5 Activation specifications. 
Their names and relationship are listed in the table.

| Bean name | Activation spec |Activation spec JNDI | Queue name | Queue JNDI |
|-----------|-----------------|---------------------|------------|------------|
| RejectedRegistrationAttemptsConsumer | RejectedRegistrationAttemptsQueueAS | jms/RejectedRegistrationAttemptsQueueAS | RejectedRegistrationAttemptsQueue| jms/RejectedRegistrationAttemptsQueue |
| HandlingEventRegistrationAttemptConsumer | HandlingEventRegistrationAttemptQueueAS | jms/HandlingEventRegistrationAttemptQueueAS | HandlingEventRegistrationAttemptQueue | jms/HandlingEventRegistrationAttemptQueue |
| CargoHandledConsumer | CargoHandledQueueAS | jms/CargoHandledQueueAS | CargoHandledQueue | jms/CargoHandledQueue |
| DeliveredCargoConsumer | DeliveredCargoQueueAS | jms/DeliveredCargoQueueAS | DeliveredCargoQueue | jms/DeliveredCargoQueue |
| MisdirectedCargoConsumer | MisdirectedCargoQueueAS | jms/MisdirectedCargoQueueAS | MisdirectedCargoQueue | jms/MisdirectedCargoQueue |

You can find the binding definition from `cargotracker/src/main/resources/META-INF/ibm-ejb-jar-bnd.xml`.

Create JMS Bus.

* Open the administrative console in your Web browser and login with WebSphere administrator credentials.
* In the left navigation panel, select **Service integration** -> **Buses**.
* In the **Buses** panel, select **New...**.
* For **Enter the name for your new bus**, fill in `CargoTrackerBus`.
* Uncheck the checkbox next to **Bus security**.
* Select **Next**.
* Select **Finish**. You'll return back to the **Buses** table.
* In the table, select **CargoTrackerBus**.
* In the **Configuration** panel, under **Topology**, select **Bus members**.
* Select **Add** button to open **Add a new bus member** panel.
* For **Select servers, cluster or WebSphere MQ server**, select **Cluster**. 
* Next to **Cluster**, from the dropdown, select **MyCluster**.
* Select **Next**.
* In **Step 1.1.1**, select **Data store**.
* Select **Next**.
* In **Step 1.1.2**, select hyperlink **MyCluster.000-CargoTrackerBus**.
    * For **Data source JNDI name**, fill in `jdbc/CargoTrackerDB`, the data source created before.
    * Select **Next**.
* Select **Next**.
* Select **Next**.
* Select **Finish**.
* Select **Save** to save the configuration.

Create JMS queue connection factories.

* In the left navigation panel, select **Resources** -> **JMS** -> **Queue connection factories**.
* Switch scope to **Cluster=MyCluster**.
* Select **New**.
  * For **Select JMS resource provider**, select **Default messaging provider**.
  * Select **OK**.
  * In **General Properties** panel, under **Administration**:
    * For **Name**, fill in `CargoTrackerQCF`.
    * For **JNDI name**, fill in `jms/CargoTrackerQCF`
  * Under **Connection**:
    * For **Bus name**, select `CargoTrackerBus`, the one created previously.
  * Select **Apply**.
  * Select **Save** to save the configuration.

Create JMS queues.

* In the left navigation panel, select **Resources** -> **JMS** -> **Queues**.
* Switch scope to **Cluster=MyCluster**.
* Follow the steps to create 5 queues, queue names and JDNI are listed in above table.
  * Select **New**.
  * For **Select JMS resource provider**, select **Default messaging provider**.
  * Select **OK**.
  * In **General Properties** panel, under **Administration**:
    * For **Name**, fill in one of queue names listed in above table, e.g. `HandlingEventRegistrationAttemptQueue`.
    * For **JNDI name**, fill in corresponding JNDI name, e.g. `jms/HandlingEventRegistrationAttemptQueue`.
  * Under **Connection**:
    * For **Bus name**, select `CargoTrackerBus`, the one created in previously.
    * For **Queue name**, select **Create Service Integration Bus Destination**. The selection causes opening a new panel. Input required value.
        * For **Identity**, input the same value with queue name, e.g. `HandlingEventRegistrationAttemptQueue`.
        * Select **Next**.
        * For **Bus member**, select **Cluster=MyCluster**.
        * Select **Next**.
        * Select **Finish**.
  * Select **Apply**.
  * Select **Save** to save the configuration. 
* After 5 queues are completed, continue to create Activation specifications.

Create Activation specifications.

* In the left navigation panel, select **Resources** -> **JMS** -> **Activation specifications**.
* Switch scope to **Cluster=MyCluster**.
* Follow the steps to create 5 activation specifications, names and JDNI are listed in above table.
  * Select **New**.
  * For **Select JMS resource provider**, select **Default messaging provider**.
  * Select **OK**.
  * In **General Properties** panel, under **Administration**:
    * For **Name**, fill in one of queue names listed in above table, e.g. `HandlingEventRegistrationAttemptQueueAS`.
    * For **JNDI name**, fill in corresponding JNDI name, e.g. `jms/HandlingEventRegistrationAttemptQueueAS`.
  * Under **Connection**:
    * For **Destination type**, select **Queue**.
    * For **Destination lookup**, fill in corresponding queue JNDI name listed in the same row of above table. In this example, value is `jms/HandlingEventRegistrationAttemptQueue`.
    * For **Connection factory lookup**, fill in `jms/CargoTrackerQCF`.
    * For **Bus name**, select `CargoTrackerBus`, the one created in previously.
  * Select **Apply**.
  * Select **Save** to save the configuration.
* After 5 activation specifications are completed, you are ready to deploy application.

## Deploy Cargo Tracker

With data source and JMS configured, you are able to deploy the application.

* Open the administrative console in your Web browser and login with WebSphere administrator credentials.
* In the left navigation panel, select **Applications** -> **Application Types** -> **WebSphere enterprise applications**.
* In the **Enterprise Applications** panel, select **Install**.
  * For **Path to the new application**, select **Local file system**.
  * Select **Choose File**, a wizard for uploading files opens.
  * Locate to `cargotracker-was-application/target/cargo-tracker.ear` and upload the EAR file.
  * Select **Next**.
  * Select **Next**.
  * In **Step 1**, select **Next**.
  * In **Step 2**:
    * Check **cargo-tracker.war** from the table.
    * Select **Apply**. 
    * Select **Next**.
  * In **Step 3**, fill in bind listeners for all the beans.
      | Bean name | Listener Bindings | Target Resource JNDI Name | Destination JNDI name |
      |-----------|-------------------|---------------------------|-----------------------|
      | RejectedRegistrationAttemptsConsumer | Activation Specification | jms/RejectedRegistrationAttemptsQueueAS | jms/RejectedRegistrationAttemptsQueue |
      | CargoHandledConsumer | Activation Specification | jms/CargoHandledQueueAS | jms/CargoHandledQueue |
      | MisdirectedCargoConsumer | Activation Specification | jms/MisdirectedCargoQueueAS | jms/MisdirectedCargoQueue |
      | DeliveredCargoConsumer | Activation Specification | jms/DeliveredCargoQueueAS | jms/DeliveredCargoQueue |
      | HandlingEventRegistrationAttemptConsumer | Activation Specification | jms/HandlingEventRegistrationAttemptQueueAS | jms/HandlingEventRegistrationAttemptQueue |
  * Select all the beans using the select all button.
  * Select **Next**.
  * In **Step 4**, check the box next to **cargo-tracker.war**. Select **Next**.
  * In **Step 5**, select **Next**.
  * In **Step 6**:
    * Check the box next to **cargo-tracker.war,WEB-INF/ejb-jar.xml**.
    * Check the box next to **cargo-tracker.war,WEB-INF/web.xml**.
    * Select **Next**.
* Select **Finish**.
* Select **Save** to save the configuration.
* In the table that lists application, select **cargo-tracker-application**.
* Select **Start** to start Cargo Tracker.

## Exercise Cargo Tracker Functionality

1. On the main page, select **Public Tracking Interface** in new window. 

   1. Enter **ABC123** and select **Track!**

   1. Observe what the **next expected activity** is.

1. On the main page, select **Administration Interface**, then, in the left navigation column select **Live** in a new window.  This opens a map view.

   1. Mouse over the pins and find the one for **ABC123**.  Take note of the information in the hover window.

1. On the main page, select **Mobile Event Logger**.  This opens up in a new, small, window.

1. Drop down the menu and select **ABC123**.  Select **Next**.

1. Select the **Location** using the information in the **next expected activity**.  Select **Next**.

1. Select the **Event Type** using the information in the **next expected activity**.  Select **Next**.

1. Select the **Voyage** using the information in the **next expected activity**.  Select **Next**.

1. Set the **Completion Date** a few days in the future.  Select **Next**.

1. Review the information and verify it matches the **next expected activity**.  If not, go back and fix it.  If so, select **Submit**.

1. Back on the **Public Tracking Interface** select **Tracking** then enter **ABC123** and select **Track**.  Observe that different. **next expected activity** is listed.

1. If desired, go back to **Mobile Event Logger** and continue performing the next activity.
