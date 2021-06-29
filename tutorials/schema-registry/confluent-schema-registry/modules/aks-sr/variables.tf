variable "PREFIX" {
  type        = string
  description = ""
}

variable "LOCATION" {
  type        = string
  description = ""
}

variable "AKS_RG_NAME" {
  type        = string
  description = ""
}

variable "AKS_VNET_CIDR" {
  type = list
}

variable "AKS_SUBNET_CIDR" {
  type = list
}

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
