# Setup the TF Environment

## Prerequisites

- Service Principal with Owner access to the Subscription that will run the TF file

  - Client ID and Client Key for the TF User account

- Service Principal with no role assignments that will be used for AKS

  - Client ID and Client Secret for the AKS Service Principal

- At least Terraform 0.12.6

- (Optional for Remote State) Latest version of Azure CLI

If you are saving state locally on your system or building from a CI system then set the following Environment Values:

```bash
export TF_VAR_TFUSER_CLIENT_ID="<Service Principal AppID that has at least Owner to the subscription>"
export TF_VAR_TFUSER_CLIENT_SECRET="<Service Principal Client Secret for TF>"
export TF_VAR_SUB_ID="<Subscription ID to deploy resources to>"
export TF_VAR_TENANT_ID="<Tenant ID to of the TF Service Principal>"

export TF_VAR_AKS_CLIENT_ID="<A Service Principal that will be used for the AKS Cluster>"
export TF_VAR_AKS_CLIENT_SECRET="<The Client Secret for the AKS Service>"
```

Change to the root directory of the Infrastructure build:

```bash
cd src/infrastructure/root
```

Now you can configure the `terraform.tfvars` to the values specific to your environment and the run `terraform init` then `terraform apply`

## Resources Created

The infrastructure provisioned by Terraform by default includes:

* 1 Resource Group
* 3 node Azure Kubernetes Cluster with Schema Registry
* 1 Azure Event Hub Namespace with autoscale enabled throughput
* 1 Azure Event Hub with 20 partitions

## Obtain Schema Registry IP

The Schema Registry IP can be obtained by running the following commands:

```bash
# Get kubeconfig to local machine
az aks get-credentials -g <resource_group_name> -n <aks_cluster_name>

# Get list of services running on the AKS cluster
kubectl get services schema-registry
NAME              TYPE        CLUSTER-IP   EXTERNAL-IP                         PORT(S)    AGE
schema-registry   ClusterIP   10.0.5.8     <SCHEMA.REGISTRY.IP.ADDRESS>        8081/TCP   102m
```

You can use this to create the schema registry URL - `http://<SCHEMA.REGISTRY.IP.ADDRESS>:8081`

## Destroy Resources

The resources can be destroyed by running the following commands:

```bash
# Preview the terraform destroy output
terraform plan -destroy

# Destroy all the resources deployed for this sample. This will ask for confirmation before destroying, unless -auto-approve is set.
terraform destroy
```