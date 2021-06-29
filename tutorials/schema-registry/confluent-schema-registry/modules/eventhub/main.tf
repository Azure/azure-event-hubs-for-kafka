resource azurerm_eventhub_namespace ehns {
  name                     = "${var.PREFIX}-ehns"
  location                 = var.LOCATION
  resource_group_name      = var.EH_RG_NAME
  sku                      = "Standard"
  capacity                 = 2
  auto_inflate_enabled     = true
  maximum_throughput_units = 20
}

resource azurerm_eventhub ehns {
  name                = "${var.PREFIX}-eh"
  namespace_name      = azurerm_eventhub_namespace.ehns.name
  resource_group_name = var.EH_RG_NAME

  partition_count   = var.PARTITION_COUNT
  message_retention = var.MSG_RETENTION
}