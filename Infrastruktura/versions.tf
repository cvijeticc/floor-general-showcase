terraform {
  required_version = ">= 1.10"

  # State je u samom AWS nalogu projekta. Projekat je klijentski i
  # ima svoj nalog bas zato da ne deli lifecycle ni pristup sa ostalima --
  # state u tudjem bucketu bi tu izolaciju ponistio.
  # use_lockfile = S3 native locking (od TF 1.10) -- nema DynamoDB tabele.
  # Bucket i AWS profil se zadaju pri init-u:
  #   terraform init -backend-config="bucket=<tfstate-bucket>" -backend-config="profile=<aws-profil>"
  backend "s3" {
    key          = "prod/terraform.tfstate"
    region       = "eu-central-1"
    encrypt      = true
    use_lockfile = true
  }

  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 6.0" }
  }
}

provider "aws" {
  region  = var.region
  profile = var.aws_profile

  default_tags {
    tags = {
      project   = "floor-general"
      env       = "prod"
      managedby = "terraform"
    }
  }
}
