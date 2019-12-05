RAMDISKDIR=/tmp/zigbee2mqtt

# init temp dir and populate it with last stored data
function initTemp() {
  rm -rf $RAMDISKDIR
  mkdir -p $RAMDISKDIR
  cat data/configuration_template.yaml | sed s/%PERMIT_JOIN%/$1/g >$RAMDISKDIR/configuration.yml
}





