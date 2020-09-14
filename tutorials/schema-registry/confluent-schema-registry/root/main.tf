provider "azurerm" {
  features {}

  subscription_id = var.SUB_ID
  client_id       = var.TFUSER_CLIENT_ID
  client_secret   = var.TFUSER_CLIENT_SECRET
  tenant_id       = var.TENANT_ID
}

resource random_integer uuid {
  min = 10
  max = 99
}

resource "azurerm_resource_group" "eh-schema-reg" {
  name     = "${var.PREFIX}${random_integer.uuid.result}-resources"
  location = var.LOCATION
}

module "aks-sr" {
  source            = "../modules/aks-sr"
  PREFIX            = "${var.PREFIX}${random_integer.uuid.result}"
  LOCATION          = var.LOCATION
  AKS_RG_NAME       = azurerm_resource_group.eh-schema-reg.name
  AKS_VNET_CIDR     = var.AKS_VNET_CIDR
  AKS_SUBNET_CIDR   = var.AKS_SUBNET_CIDR
  NP1_NAME          = var.NP1_NAME
  NP1_NODE_COUNT    = var.NP1_NODE_COUNT
  NP1_MIN_COUNT     = var.NP1_MIN_COUNT
  NP1_MAX_COUNT     = var.NP1_MAX_COUNT
  NP1_NODE_SIZE     = var.NP1_NODE_SIZE
  NP1_MAX_PODS      = var.NP1_MAX_PODS
  AKS_CLIENT_ID     = var.AKS_CLIENT_ID
  AKS_CLIENT_SECRET = var.AKS_CLIENT_SECRET
}

module "eventhub" {
  source          = "../modules/eventhub"
  PREFIX          = "${var.PREFIX}${random_integer.uuid.result}"
  LOCATION        = var.LOCATION
  EH_RG_NAME      = azurerm_resource_group.eh-schema-reg.name
  PARTITION_COUNT = var.EH_PARTITION_COUNT
  MSG_RETENTION   = var.EH_MSG_RETENTION
}