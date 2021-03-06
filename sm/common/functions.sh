# Function calculates netmask from number of bits
#
cidr2mask() {
  local i mask=""
  local full_octets=$(($1/8))
  local partial_octet=$(($1%8))

  for ((i=0;i<4;i+=1)); do
    if [ $i -lt $full_octets ]; then
      mask+=255
    elif [ $i -eq $full_octets ]; then
      mask+=$((256 - 2**(8-$partial_octet)))
    else
      mask+=0
    fi  
    test $i -lt 3 && mask+=.
  done

  echo $mask
}

# Function calculates number of bit in a netmask
#
mask2cidr() {
    nbits=0
    IFS=.
    for dec in $1 ; do
        case $dec in
            255) let nbits+=8;;
            254) let nbits+=7;;
            252) let nbits+=6;;
            248) let nbits+=5;;
            240) let nbits+=4;;
            224) let nbits+=3;;
            192) let nbits+=2;;
            128) let nbits+=1;;
            0);;
            *) echo "Error: $dec is not recognised"; exit 1
        esac
    done
    echo "$nbits"
}

# Add sources to network interface in ubuntu
add_source() {
	echo "add_source called"
	INTERFACES=/etc/network/interfaces
	if ! grep -q "^source\ $INTERFACES.d/\*.cfg" $INTERFACES; then
		echo "source $INTERFACES.d/*.cfg" >> $INTERFACES
	fi
}

#$1 file $2:line
line_in_file() {
	while IFS='' read -r line || [[ -n $line ]]; do
		if [[ $line == $2 ]]; then
			return 0
		fi
	done < "$1"
	return 1
}

# Read the file in parameter and fill the array named "array"
getArray() {
    i=0
    while read line # Read a line
    do
        array[i]=$line # Put it into the array
        i=$(($i + 1))
    done < $1
}
