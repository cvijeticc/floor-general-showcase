# 10.30.0.0/16 namerno NE preklapa nase druge VPC-ove (10.0.0.0/16 i
# 10.20.0.0/16). Ako ikad zatreba peering (npr. migracija baze sa dev boxa),
# CIDR sudar bi ga onemogucio, a VPC CIDR se posle ne menja -- pravi se nov VPC.
resource "aws_vpc" "main" {
  cidr_block           = "10.30.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = var.name }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = var.name }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.30.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = { Name = "${var.name}-public" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "${var.name}-public" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_security_group" "app" {
  name        = "${var.name}-app"
  description = "Javni HTTP/HTTPS; baza i app portovi samo lokalno"
  vpc_id      = aws_vpc.main.id

  tags = { Name = "${var.name}-app" }
}

# 80 ostaje otvoren zbog certbot HTTP-01 challenge-a i redirekcije na 443.
resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  description       = "HTTP (redirect na 443 + certbot)"
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  description       = "HTTPS"
}

# Uslovno -- podrazumevano se NE kreira (ssh_ingress_cidr = ""). Shell ide
# preko SSM-a; ovo je samo za slucaj da zatreba klasican sshd.
resource "aws_vpc_security_group_ingress_rule" "ssh" {
  count = var.ssh_ingress_cidr == "" ? 0 : 1

  security_group_id = aws_security_group.app.id
  cidr_ipv4         = var.ssh_ingress_cidr
  from_port         = 22
  to_port           = 22
  ip_protocol       = "tcp"
  description       = "SSH sa odobrene adrese"
}

# PostgreSQL i app portovi (8080, 3000) NEMAJU ingress pravilo -- vezuju se na
# 127.0.0.1 u compose-u, a spolja se dolazi samo kroz nginx.
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.app.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
  description       = "Sav izlazni saobracaj"
}
