
declare -a PROPConfig=("0" "1" "2" "3" "4" "5" "6")
declare -a SEARCHConfig=("0" "1" "2" "3")
timeout=900
problem="rcpsp"  # TODO change the name of the problem
# TODO put the path / command to launch the executable
javaPath="/Library/Java/JavaVirtualMachines/jdk-17.0.2.jdk/Contents/Home/bin/java"
launch_solver="java -Dfile.encoding=UTF-8 -jar 'out/artifacts/SDHEEF2024_jar/SDHEEF2024.jar'"

currentTime=`date +"%Y-%m-%d_%H-%M-%S"`
resultFile="$problem-$currentTime.txt"
# loop through the configurations
for prop in "${PROPConfig[@]}"
do
  for search in "${SEARCHConfig[@]}"
  do
    # extract the instances from the data folder and run the solver
    find Data/ -type f | parallel $launch_solver {} $prop $search $timeout >> $resultFile
  done
done
