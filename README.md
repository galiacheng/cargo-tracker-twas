# Run a Java EE 7 application in IBM WebSphere Application Server Traditional Network Deployment V9 on Azure VMs

The project demonstrates how you can configure and run a Java EE 7 application in IBM WebSphere Application Server Traditional (tWAS) Network Deployment V9 on Azure VMs. The project is directly based on the well known Jakata EE sample application Cargo Tracker V1.0. For further details on the project, please visit: https://eclipse-ee4j.github.io/cargotracker/.

# Getting Started

Environment setup：

* An Azure subscription.
* Get the project source code.
* Ensure you are running Java SE 8.
* Make sure JAVA_HOME is set.
* Make sure Maven is set up properly.

Go to the project root and run `mvn clean install` to build the application. You will get:

* A WAR package in `cargotracker/target/cargo-tracker.war`.
* An EAR package in `cargotracker-was-application/target/cargo-tracker.ear`, which will be deployed to tWAS.

## Standup WebSphere Application Server (traditional) Cluster on Azure VMs

The project leverages [Azure Marketplace offer for WebSphere Application Server](https://aka.ms/websphere-on-azure-portal) to standup a tWAS cluster. Follow [Deploy WebSphere Application Server (traditional) Cluster on Azure Virtual Machines](https://learn.microsoft.com/azure/developer/java/ee/traditional-websphere-application-server-virtual-machines?tabs=basic), you will deploy a cluster with 4 members. Write down your credentials for WebSpere administrator.

After the deployment finishes, select the **Outputs** section on the left panel and write down the administrative console and IHS console URLs.

Return back to this project.

## Create Azure Database for PostgreSQL Flexible Server

Cargo Tracker requires a database for persistence. This project creates a PostgreSQL Flexible Server for it. Follow [Quickstart: Create an Azure Database for PostgreSQL - Flexible Server instance in the Azure portal](https://learn.microsoft.com/azure/postgresql/flexible-server/quickstart-create-server-portal), you will create a PostgreSQL Flexible Server with a database `mypgsqldb`.

After the database is up, continue to configure Firewall rules to allow access from WebSphere servers.

* Open the resource group that has PostgreSQL Flexible Server created.
* Select the PostgreSQL Flexible Server.
* Select **Settings** -> **Connection security** -> **Firewall rules**.
* Next to **Allow access to Azure services**, select **Yes**.
* Select **Save** to save the config.

Write down the connection information:

* Server name.
* Server admin login name.
* Server admin password.

## Install PostgreSQL driver in tWAS

TBD

## Create data source connetion in tWAS

In this section, you'll configure the data source using IBM console. 

Before configuring the data source, you need to create authentication alias for the PostgreSQL Server admin credentials. Follow the steps to create J2C authentication data.

* Open the administrative console in your Web browser and login with WebSphere administrator credentials.
* In the left navigation panel, select **Resources** -> **Security**. You will open **Global security** panel.
* Under the **Authentication** section, expand **Java Authentication and Authorization Service**.
* Select **J2C authentication data**.
* Select **New...** button and input values:
  * For **Alias**, fill in `postgresql-cargotracker-auth`. 
  * For **User ID**, fill in PostgreSQL Server admin login name. 
  * For **Password**, fill in PostgreSQL Server admin login password.
  * Select **Apply**.
  * Select **OK**. You will find the authentication listed in the table.
  * Select **Apply**.
  * Select **Save** to save the configuration.You will return back to the **Global security**.
  * Select **Apply**.
  * Select **Save** to save the configuration.

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
    * Select *Next**.
  * In **Step 4**:
    * Under **Component-managed authentication alias**, select the authentication alias `postgresql-cargotracker-auth`.
    * Select **Next**.
  * In **Summary**, select **Finish**.
  * Select **Save** to save the configuration.
  * Select the data source **CargoTrackerDB** in the table, continue to configure URL.
    * Select **Custom properties**. From the table, from column **Name**, find the row whose name is **URL**.
    * Select **URL**. Fill in value with `jdbc:postgresql://<postgresql-server-name>:5432/mypgsqldb`, replace `<postgresql-server-name>` with the PostgreSQL Server name you wrote down. 
    * Select **Apply**. 
    * Select **OK**.
    * Select **Save** to save the configuration.

Validate the data source connection:

  * Select hyperlink **CargoTrackerDB** to return back to the **Data source** panel.
  * Select **Test connection**. If the data source configuration is correct, you will find message like "The test connection operation for data source CargoTrackerDB on server nodeagent at node managed6ba334VM2Node01 was successful". If there is any error, resolve it before you move on.

## Create JMS queues

In this section, you'll create a JMS Bus, a JMS Queue Connection Factory, five JMS Queues and 5 Activation specifications. 
Their names ane relationship are listed in the table.

| Bean name | Activation spec |Activation spec JNDI | Queue name | Queue JNDI |
|-----------|-----------------|---------------------|------------|------------|
| RejectedRegistrationAttemptsConsumer | RejectedRegistrationAttemptsQueueAS | jms/RejectedRegistrationAttemptsQueueAS | RejectedRegistrationAttemptsQueue| jms/RejectedRegistrationAttemptsQueue |
| HandlingEventRegistrationAttemptConsumer | HandlingEventRegistrationAttemptQueueAS | jms/HandlingEventRegistrationAttemptQueueAS | HandlingEventRegistrationAttemptQueue | jms/HandlingEventRegistrationAttemptQueue |
| CargoHandledConsumer | CargoHandledQueueAS | jms/CargoHandledQueueAS | CargoHandledQueue | jms/CargoHandledQueue |
| DeliveredCargoConsumer | DeliveredCargoConsumerAS | jms/DeliveredCargoConsumerAS | DeliveredCargoConsumer | jms/DeliveredCargoConsumer |
| MisdirectedCargoConsumer | MisdirectedCargoConsumerAS | jms/MisdirectedCargoConsumerAS | MisdirectedCargoConsumer | jms/MisdirectedCargoConsumer |

Create JMS Bus.

* Open the administrative console in your Web browser and login with WebSphere administrator credentials.
* In the left navigation panel, select **Service integration** -> **Buses**.
* In the **Buses** panel, select **New...**.
* For **Enter the name for your new bus**, fill in `CargoTrackerBus`.
* Uncheck the checkbox next to **Bus security**.
* Select **Next**.
* Select **Finish**. You'll return back to the Buses table.
* In the table, select **CargoTrackerBus**.
* In the **Configuration** panel, under **Topology**, select **Bus members**.
* Select **Add** button to open **Add a new bus member** panel.
* For **Select servers, cluster or WebSphere MQ server**, select **Cluster**. 
* Next to **Cluster**, from the dropdown, select **MyCluster**.
* Select **Next**.
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
    * For **Bus name**, select `CargoTrackerBus`, the one created in previously.
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
        * For **Identity**, input the same value with queue name, e.g `HandlingEventRegistrationAttemptQueue`.
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
* In the left navigation panel, select **Applications** -> **Applications Types** -> **WebSphere enterprise applications**.
* In the **Enterprise Applications** panel, select **Install**.
  * For **Path to the new application**, select **Local file system**.
  * Select **Choose File**, a wizard for uploading files will be open.
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
      | MisdirectedCargoConsumer | Activation Specification | jms/MisdirectedCargoConsumerAS | jms/MisdirectedCargoConsumer |
      | DeliveredCargoConsumer | Activation Specification | jms/DeliveredCargoConsumerAS | jms/DeliveredCargoConsumer |
      | HandlingEventRegistrationAttemptConsumer | Activation Specification | jms/HandlingEventRegistrationAttemptQueueAS | jms/HandlingEventRegistrationAttemptQueue |
  * Select all the beans using the select all button.
  * Select **Next**.
  * In **Step 4**, check the box next to **cargo-tracker.war**. Select **Next**.
  * In **Step 5**, select **Next**.
  * In **Step 6**:
    * Check the box next to **cargo-tracker.war,WEB-INF/ejb-jar.xml**.
    * Check the box next to **	cargo-tracker.war,WEB-INF/web.xml**.
    * Select **Next**.
* Select **Finish**.
* Select **Save** to save the configuration.
* In the table that lists application, select **cargo-tracker-application**.
* Select **Start** to start Cargo Tracker.

## Test the appliation
