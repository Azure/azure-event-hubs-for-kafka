PREFIX             = "ehsr"
LOCATION           = "westus2"
AKS_VNET_CIDR      = ["10.1.0.0/16"]
AKS_SUBNET_CIDR    = ["10.1.0.0/21"]
NP1_NAME           = "workerpool"
NP1_NODE_COUNT     = "3"
NP1_MIN_COUNT      = "3"
NP1_MAX_COUNT      = "12"
NP1_MAX_PODS       = "100"
NP1_NODE_SIZE      = "Standard_D8s_v3"
EH_PARTITION_COUNT = 20
EH_MSG_RETENTION   = 1