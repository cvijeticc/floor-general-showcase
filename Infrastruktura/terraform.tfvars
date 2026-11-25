# Port 22 je ZATVOREN. Shell ide preko SSM Session Managera -- developer ulazi
# svojim Identity Center identitetom (permission set za ovaj nalog),
# bez ijednog deljenog kljuca, i svaka sesija je u CloudTrail-u.
#
# Popuni ovo ("x.x.x.x/32") samo ako ti bas zatreba klasican sshd.
ssh_ingress_cidr = ""
