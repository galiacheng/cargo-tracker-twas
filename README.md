# Run a Java EE 7 (Cargo Tracker V1.0) in IBM WebSphere Application Server Traditional Network Deployment V9 on Azure VMs

The project demonstrates how you can configure and run a Java EE 7 application in IBM WebSphere Application Server Traditional (tWAS) Network Deployment V9 on Azure VMs. The project is directly based on the well known Jakata EE sample application Cargo Tracker. For further details on the project, please visit: https://eclipse-ee4j.github.io/cargotracker/.

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
    * From the drop down, select **PostgreSQLJDBCProvider**.
    * Select **Next**.
  * In **Step 3**:
    * For **Data store helper class name**, fill in **com.ibm.websphere.rsadapter.GenericDataStoreHelper**.
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

## Deploy Cargo Tracker

## Test the appliation
