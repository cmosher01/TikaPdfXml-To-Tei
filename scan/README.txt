lsusb
Bus 004 Device 002: ID 04c5:132b Fujitsu, Ltd ScanSnap iX500
Bus 003 Device 002: ID 04a9:1913 Canon, Inc. LiDE 300



scanimage --list-devices
device `pixma:04A91913_4E4C1B' is a CANON CanoScan LiDE 300 multi-function peripheral
device `fujitsu:ScanSnap iX500:1637946' is a FUJITSU ScanSnap iX500 scanner


scanimage --help -d 'pixma:04A91913_4E4C1B'

scanimage --help -d 'fujitsu:ScanSnap iX500:1637946'



scanimage --verbose --progress --format=jpeg --resolution=2400 >testscan.jpg
scanimage --format=jpeg --mode=color --page-height=357mm >cartradein.jpg
