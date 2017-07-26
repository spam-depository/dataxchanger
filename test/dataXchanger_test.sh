#!/bin/bash

# Copyright 2013 Kornelius Podranski
#
# This file is part of dataXchanger.
#
# dataXchanger is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# dataXchanger is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with dataXchanger.  If not, see <http://www.gnu.org/licenses/>.


# this script runs some basic test on the dataXchanger tool.

#ENVIRONMENT
JAVA=java
EXE="$JAVA -jar dataXchanger.jar"
FTPSERVER=your.ftp.server.com
FTPPORT=21
FTPUSER=yourftpuser
FTPPWD=yourftppassword
LOGFILE=dataXchanger_test.log

#calld once upon start of this scrip
init() {
	#make sure nothing is left from last run
	clean
	rm -f "$LOGFILE"
	
	#create keypair if not there
	if [ ! -e rsa_private_0.pem ]; then
		if [ -e rsa_public_0.pem ]; then
			rm -f rsa_public_0.pem
		fi
		openssl genrsa 4096 2> /dev/null | openssl pkcs8 -nocrypt -topk8 -outform PEM -out rsa_private_0.pem
	fi
		if [ ! -e rsa_public_0.pem ]; then
		openssl rsa -in rsa_private_0.pem -out rsa_public_0.pem -pubout 2> /dev/null
	fi
	if [ ! -e rsa_private_1.pem ]; then
		if [ -e rsa_public_1.pem ]; then
			rm -f rsa_public_1.pem
		fi
		openssl genrsa 4096 2> /dev/null | openssl pkcs8 -nocrypt -topk8 -outform PEM -out rsa_private_1.pem
	fi
	if [ ! -e rsa_public_1.pem ]; then
		openssl rsa -in rsa_private_1.pem -out rsa_public_1.pem -pubout 2> /dev/null
	fi
}

#called beore each test
setup() {
	mkdir send
	mkdir receive
	mkdir receive_1
	cp rsa_private_0.pem receive/
	cp rsa_private_1.pem receive_1/
	cp rsa_public_0.pem send/
	cp rsa_public_1.pem send/
	cp test.dcm send/
	cp whitelist send/
	cp dataXchanger.jar send/
	cp dataXchanger.jar receive/
	cp dataXchanger.jar receive_1/
}

#called after each test
clean() {
	rm -rf send
	rm -rf receive
	rm -rf receive_1
}

#call the specific $EXE commandline
#arguments:
#	$1 directory to change into before execution
#	$2..$n arguments for $EXE 
execute() {
	dir=$1
	shift
	
	echo "pwd = \"$dir\"" >> "$LOGFILE"
	echo "$EXE" "$@" >> "$LOGFILE"
	
	(cd "$dir"; $EXE "$@")
	return $?
}

#call to run a test
#makes setup and clean as well as the logging and indication of test result (exit code)
#arguments:
#	$1 name of the test-function to run
run() {
	setup
	echo "running $1------------------------------------------------------------" >> "$LOGFILE"
	printf "$1\t"
	eval $1 >> "$LOGFILE" 2>&1
	if [ $? -ne 0 ]; then
		result=failed
	else
		result=passed
	fi
	echo "$1 $result############################################################" >> "$LOGFILE"
	echo $result
	clean
}

test_send_and_receive_single() {
	#send
	out=$(execute send --send --whitelist whitelist --enc-key rsa_public_0.pem \
		--ftp-server $FTPSERVER --ftp-active --ftp-user $FTPUSER \
		--ftp-password $FTPPWD --debug --input test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi

	#transfer config-file
	echo "cp send/encrypted_test.dcm_0.rconf receive/test.rconf"
	cp send/test_0.rconf receive/test.rconf

	#receive
	out=$(execute receive --receive --ftp-active --dec-key rsa_private_0.pem\
		--debug --conf test.rconf 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi
	if [ ! -e receive/test.dcm ]; then
		return 1
	fi
	
	#compare result with anonymized file from sender
	echo "cmp send/anonymized_test.dcm receive/test.dcm"
	out=$(cmp send/anonymized_test.dcm receive/test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	
	#return secessfully
	return 0
}

test_help() {
	out=$(execute . --help 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -z "$out" ]; then
		return 1
	fi
}

test_version() {
	out=$(execute . --version 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -z "$out" ]; then
		return 1
	fi
}

test_corrupt_symkey_in_rconf() {
	#send
	out=$(execute send --send --whitelist whitelist --enc-key rsa_public_0.pem \
		--ftp-server $FTPSERVER --ftp-active --ftp-user $FTPUSER --ftp-password $FTPPWD --input test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi

	#transfer config-file and switch chars
	echo "sed 's/\(^xskey=....................\)\(.\)\(.\)/\1\3\2/' send/encrypted_test.dcm_0.rconf > receive/test.rconf"
	sed 's/\(^xskey=....................\)\(.\)\(.\)/\1\3\2/' send/encrypted_test.dcm_0.rconf > receive/test.rconf

	#receive
	echo "execute receive --receive --dec-key rsa_private.pem --debug --conf test.rconf"
	out=$(execute receive --receive --ftp-active --dec-key rsa_private_0.pem --debug --conf test.rconf 2>&1)
	return=$?
	echo "$out"
	if [ $return -eq 0 ]; then
		return 1
	fi
	if [ -z "$out" ]; then
		return 1
	fi
	if [ -e receive/decrypted_* ]; then
		return 1
	fi
	return 0
}

test_send_and_receive_multi() {
	#send
	out=$(execute send --send --whitelist whitelist --enc-key rsa_public_0.pem \
		--enc-key rsa_public_1.pem --ftp-server $FTPSERVER --ftp-active \
		--ftp-user $FTPUSER --ftp-password $FTPPWD --debug --input test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi

	#transfer config-files
	echo "cp send/test_0.rconf receive/test.rconf"
	cp send/test_0.rconf receive/test.rconf
	echo "cp send/test_1.rconf receive_1/test.rconf"
	cp send/test_1.rconf receive_1/test.rconf

	#receive_0
	out=$(execute receive --receive --ftp-active --dec-key rsa_private_0.pem --debug --conf test.rconf 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi
	if [ ! -e receive/test.dcm ]; then
		return 1
	fi
	#compare result with anonymized file from sender
	echo "cmp send/anonymized_test.dcm receive/test.dcm"
	out=$(cmp send/anonymized_test.dcm receive/test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	
	#receive_1
	out=$(execute receive_1 --receive --ftp-active --dec-key rsa_private_1.pem --debug --conf test.rconf 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	if [ -n "$out" ]; then
		return 1
	fi
	if [ ! -e receive_1/test.dcm ]; then
		return 1
	fi
	#compare result with anonymized file from sender
	echo "cmp send/anonymized_test.dcm receive_1/test.dcm"
	out=$(cmp send/anonymized_test.dcm receive_1/test.dcm 2>&1)
	return=$?
	echo "$out"
	if [ $return -ne 0 ]; then
		return $return
	fi
	
	#return secessfully
	return 0
}

#main
init

run test_version
run test_help
run test_send_and_receive_single
run test_send_and_receive_multi
run test_corrupt_symkey_in_rconf
