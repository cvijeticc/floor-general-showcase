data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_caller_identity" "current" {}

# Ubuntu 24.04 LTS (noble), ARM64 -- za t4g/Graviton. Isti distro kao na
# ostalim nasim masinama, da se navike i skripte poklapaju.
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd*/ubuntu-noble-24.04-arm64-server-*"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}
