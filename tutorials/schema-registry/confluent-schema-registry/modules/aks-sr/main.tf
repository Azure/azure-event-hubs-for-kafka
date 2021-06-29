resource "azurerm_log_analytics_workspace" "aks_sr_workspace" {
  name                = "${var.PREFIX}-srworkspace"
  location            = var.LOCATION
  resource_group_name = var.AKS_RG_NAME
  sku                 = "PerGB2018"
}

resource "azurerm_log_analytics_solution" "aks_sr_log_solution" {
  solution_name         = "Containers"
  location              = var.LOCATION
  resource_group_name   = var.AKS_RG_NAME
  workspace_resource_id = azurerm_log_analytics_workspace.aks_sr_workspace.id
  workspace_name        = azurerm_log_analytics_workspace.aks_sr_workspace.name

  plan {
    publisher = "Microsoft"
    product   = "OMSGallery/Containers"
  }
}

resource "azurerm_virtual_network" "aksnet" {
  name                = "${var.PREFIX}-network"
  location            = var.LOCATION
  resource_group_name = var.AKS_RG_NAME
  address_space       = var.AKS_VNET_CIDR #Needs to be a list datatype
}

resource "azurerm_subnet" "aksnet" {
  name                 = "aks-subnet"
  resource_group_name  = var.AKS_RG_NAME
  address_prefixes     = var.AKS_SUBNET_CIDR
  virtual_network_name = azurerm_virtual_network.aksnet.name
}


resource "azurerm_kubernetes_cluster" "schema_reg" {
  name                = "${var.PREFIX}-aks-schema-reg"
  location            = var.LOCATION
  dns_prefix          = "${var.PREFIX}-aks-schema-reg"
  resource_group_name = var.AKS_RG_NAME

  default_node_pool {
    name                  = var.NP1_NAME
    node_count            = var.NP1_NODE_COUNT
    vm_size               = var.NP1_NODE_SIZE
    type                  = "VirtualMachineScaleSets"
    enable_auto_scaling   = true
    min_count             = var.NP1_MIN_COUNT
    max_count             = var.NP1_MAX_COUNT
    max_pods              = var.NP1_MAX_PODS
    vnet_subnet_id        = azurerm_subnet.aksnet.id
  }

  service_principal {
    client_id     = var.AKS_CLIENT_ID
    client_secret = var.AKS_CLIENT_SECRET
  }

  addon_profile {
    kube_dashboard {
      enabled = true
    }
    oms_agent {
      enabled                    = true
      log_analytics_workspace_id = azurerm_log_analytics_workspace.aks_sr_workspace.id
    }
  }

  network_profile {
    network_plugin = "azure"
  }
}

provider "helm" {
  version = ">= 0.7"

  kubernetes {
    host                   = azurerm_kubernetes_cluster.schema_reg.kube_config.0.host
    client_certificate     = base64decode(azurerm_kubernetes_cluster.schema_reg.kube_config.0.client_certificate)
    client_key             = base64decode(azurerm_kubernetes_cluster.schema_reg.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(azurerm_kubernetes_cluster.schema_reg.kube_config.0.cluster_ca_certificate)
  }
}

resource "helm_release" "kafka" {
  name       = "schema-registry"
  repository = "http://storage.googleapis.com/kubernetes-charts-incubator"
  chart      = "schema-registry"
  namespace  = "default"

  set {
    name  = "external.servicePort"
    value = "8081"
  }

  set {
    name = "external.enabled"
    value = "true"
  }
}
