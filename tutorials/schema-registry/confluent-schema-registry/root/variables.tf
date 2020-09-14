# Base Variables for all modules
variable "TFUSER_CLIENT_ID" {
  type        = string
  description = ""
}

variable "TFUSER_CLIENT_SECRET" {
  type        = string
  description = ""
}

variable "SUB_ID" {
  type        = string
  description = ""
}

variable "TENANT_ID" {
  type        = string
  description = ""
}


variable "PREFIX" {

}

variable "LOCATION" {

}

# Variables for Network
variable "AKS_VNET_CIDR" {
  type = list
}

variable "AKS_SUBNET_CIDR" {
  type = list
}

# Variables needed for AKS Module
variable "NP1_NAME" {
  type        = string
  description = ""
}

variable "NP1_NODE_COUNT" {
  type        = string
  description = ""
}

variable "NP1_MIN_COUNT" {
  type        = string
  description = ""
}

variable "NP1_MAX_COUNT" {
  type        = string
  description = ""
}

variable "NP1_MAX_PODS" {
  type        = string
  description = ""
}

variable "NP1_NODE_SIZE" {
  type        = string
  description = ""
}

variable "AKS_CLIENT_ID" {
  type        = string
  description = ""
}

variable "AKS_CLIENT_SECRET" {
  type        = string
  description = ""
}

# Event Hub Specific variables

variable "EH_PARTITION_COUNT" {
  type        = number
  default     = 2
  description = ""
}

variable "EH_MSG_RETENTION" {
  type        = number
  default     = 1
  description = ""
}
