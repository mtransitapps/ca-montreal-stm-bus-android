#!/bin/bash
source ../commons/commons.sh
echo ">> Building...";

DIRECTORY=$(basename ${PWD});
CUSTOM_SETTINGS_GRADLE_FILE="../settings.gradle.all";

IS_CI=false;
if [[ ! -z "${CI}" ]]; then
	IS_CI=true;
fi
echo "$DIRECTORY/build.sh > IS_CI:'${IS_CI}'";

GRADLE_ARGS="";
if [[ ${IS_CI} = true ]]; then
	GRADLE_ARGS=" --console=plain";
fi

SETTINGS_FILE_ARGS="";
if [[ -f ${CUSTOM_SETTINGS_GRADLE_FILE} ]]; then
	SETTINGS_FILE_ARGS=" -c $CUSTOM_SETTINGS_GRADLE_FILE"; #--settings-file
fi

echo ">> Gradle cleaning...";
../gradlew ${SETTINGS_FILE_ARGS} clean ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};
echo ">> Gradle cleaning... DONE";

echo ">> Running bundle release...";
../gradlew ${SETTINGS_FILE_ARGS} :${DIRECTORY}:bundleRelease ${GRADLE_ARGS};
RESULT=$?;
checkResult ${RESULT};
echo ">> Running bundle release... DONE";

CUSTOM_LOCAL_PROPERTIES="../custom_local.properties";
if [ -f "$CUSTOM_LOCAL_PROPERTIES" ]; then
	echo ">> Copying release bundles to output dir...";
	while IFS='=' read -r key value; do
		key=$(echo $key | tr '.' '_')
		eval ${key}=\${value}
	done < "$CUSTOM_LOCAL_PROPERTIES"
	if [[ ! -z "${output_dir}" ]]; then
		cp build/outputs/bundle/release/*.aab ${output_dir};
		RESULT=$?;
		checkResult ${RESULT};
	fi
	if [[ ! -z "${output_cloud_dir}" ]]; then
		cp build/outputs/bundle/release/*.aab ${output_cloud_dir};
		RESULT=$?;
		checkResult ${RESULT};
	fi
	echo ">> Copying release bundles to output dir... DONE";
fi

echo ">> Building... DONE";
exit ${RESULT};
