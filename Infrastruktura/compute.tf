# Pristup bez kljuceva i bez otvorenog porta 22: SSM Session Manager.
resource "aws_iam_role" "app" {
  name = "${var.name}-app"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.name}-app"
  role = aws_iam_role.app.name
}

# Postgres podaci, docker volumeni, dumpovi. Odvojen od root diska da prezive
# zamenu instance (promena tipa na m7g.large, nov AMI).
resource "aws_ebs_volume" "data" {
  availability_zone = aws_subnet.public.availability_zone
  size              = var.data_volume_size
  type              = "gp3"
  encrypted         = true

  tags = {
    Name   = "${var.name}-data"
    Backup = "daily" # hvata ga DLM politika iz backup.tf
  }

  # Podaci lige: Terraform ne sme da ih obrise ni greskom ni refaktorom.
  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.app.id
}

# Stabilna adresa: DNS zapis lige pokazuje ovde i prezivljava stop/start
# (promena tipa instance, reboot). Naplata je ista kao za auto-dodeljenu IPv4.
resource "aws_eip" "app" {
  domain = "vpc"
  tags   = { Name = var.name }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}

resource "aws_instance" "app" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = var.instance_type
  subnet_id     = aws_subnet.public.id
  key_name      = var.key_pair_name == "" ? null : var.key_pair_name

  vpc_security_group_ids = [aws_security_group.app.id]
  iam_instance_profile   = aws_iam_instance_profile.app.name

  # Podrazumevano se NE koristi (use_spot = false) -- produkcija.
  dynamic "instance_market_options" {
    for_each = var.use_spot ? [1] : []
    content {
      market_type = "spot"
      spot_options {
        spot_instance_type             = "persistent"
        instance_interruption_behavior = "stop"
      }
    }
  }

  root_block_device {
    volume_size = var.root_volume_size
    volume_type = "gp3"
    encrypted   = true
  }

  metadata_options {
    http_tokens                 = "required" # IMDSv2 obavezan
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 2 # docker kontejneri dosezu IMDS
  }

  # file() a ne templatefile() -- skripta ne prima promenljive, a templatefile
  # bi svaki ${...} iz basha pokusao da protumaci kao Terraform izraz.
  user_data = file("${path.module}/user-data/bootstrap.sh")

  # Izmena bootstrap skripte ne sme da rekreira masinu na kojoj radi liga.
  user_data_replace_on_change = false

  tags = { Name = var.name }

  lifecycle {
    ignore_changes = [ami] # nov Ubuntu AMI ne sme sam da zameni produkciju
  }
}
