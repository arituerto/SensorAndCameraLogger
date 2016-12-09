% This script loads and synchronyces data acquired with the acqPlatform
% package in android.

close all;
clear all;

datasetFolder = '/mnt/DATA/Datasets/androidDatasets/datasets';

collectionName = '2016-12-05_152331_samsung_SM-G925F_10steps';

imgres = '640x480';
imgext = '.jpg';



% Read the sensors log files
listOfLogFiles = dir(sprintf('%s/%s/sensor_*_log.csv',datasetFolder,collectionName));
for it_log = 1:length(listOfLogFiles)
    datatype = listOfLogFiles(it_log).name(8:end-8);
    datatable = readtable(...
        sprintf('%s/%s/%s',datasetFolder,collectionName,listOfLogFiles(it_log).name),...
        'ReadVariableNames',false,...
        'HeaderLines',1);
    if (~isempty(datatable))
        switch datatype
            % PHONE SENSORS
            case 'ACCELEROMETER'
                acc.sysTime = table2array(datatable(:,1));
                acc.evntTime = table2array(datatable(:,2));
                acc.value = table2array(datatable(:,3:end));
            case 'LINEAR_ACCELERATION'
                lacc.sysTime = table2array(datatable(:,1));
                lacc.evntTime = table2array(datatable(:,2));
                lacc.value = table2array(datatable(:,3:end));
            case 'GRAVITY'
                grv.sysTime = table2array(datatable(:,1));
                grv.evntTime = table2array(datatable(:,2));
                grv.value = table2array(datatable(:,3:end));
            case 'GYROSCOPE'
                gyr.sysTime = table2array(datatable(:,1));
                gyr.evntTime = table2array(datatable(:,2));
                gyr.value = table2array(datatable(:,3:end));
            case 'GAME_ROTATION_VECTOR'
                grv.sysTime = table2array(datatable(:,1));
                grv.evntTime = table2array(datatable(:,2));
                grv.value = table2array(datatable(:,3:end));
            case 'ROTATION_VECTOR'
                rv.sysTime = table2array(datatable(:,1));
                rv.evntTime = table2array(datatable(:,2));
                rv.value = table2array(datatable(:,3:end));
                % CPRO R EXTERTAL SENSOR
            case 'CPRO_R_ACCELEROMETER'
                cR_acc.sysTime = table2array(datatable(:,1));
                cR_acc.evntTime = table2array(datatable(:,2));
                cR_acc.value = table2array(datatable(:,3:end));
            case 'CPRO_R_BAROMETER'
                cR_bar.sysTime = table2array(datatable(:,1));
                cR_bar.evntTime = table2array(datatable(:,2));
                cR_bar.value = table2array(datatable(:,3:end));
            case 'CPRO_R_GYROSCOPE'
                cR_gyr.sysTime = table2array(datatable(:,1));
                cR_gyr.evntTime = table2array(datatable(:,2));
                cR_gyr.value = table2array(datatable(:,3:end));
            case 'CPRO_R_STEPS'
                cR_stp.sysTime = table2array(datatable(:,1));
                cR_stp.evntTime = table2array(datatable(:,2));
                cR_stp.value = table2array(datatable(:,3:end));
                % CPRO R EXTERTAL SENSOR
            case 'CPRO_L_ACCELEROMETER'
                cL_acc.sysTime = table2array(datatable(:,1));
                cL_acc.evntTime = table2array(datatable(:,2));
                cL_acc.value = table2array(datatable(:,3:end));
            case 'CPRO_L_BAROMETER'
                cL_bar.sysTime = table2array(datatable(:,1));
                cL_bar.evntTime = table2array(datatable(:,2));
                cL_bar.value = table2array(datatable(:,3:end));
            case 'CPRO_L_GYROSCOPE'
                cL_gyr.sysTime = table2array(datatable(:,1));
                cL_gyr.evntTime = table2array(datatable(:,2));
                cL_gyr.value = table2array(datatable(:,3:end));
            case 'CPRO_L_STEPS'
                cL_stp.sysTime = table2array(datatable(:,1));
                cL_stp.evntTime = table2array(datatable(:,2));
                cL_stp.value = table2array(datatable(:,3:end));
            case 'CAMERA'
                cL_stp.sysTime = table2array(datatable(:,1));
                cL_stp.evntTime = table2array(datatable(:,2));
                cL_stp.name = table2array(datatable(:,3:end));                
        end
    end
end

% Read images
listOfImg = dir(sprintf('%s/%s/images_%s/*%s',datasetFolder,...
    collectionName,imgres, imgext));
imgTime = zeros(size(listOfImg));
for it_img = 1:length(listOfImg)
    index = [find(listOfImg(it_img).name == '_') ...
        find(listOfImg(it_img).name == '.')];
    imgTime(it_img) = (1/1000000000)*(str2double(...
        listOfImg(it_img).name(index(1)+1:index(2)-1)));
end

% Acquisition times
min_sys_time = min([[sensorData.sysTime] imgTime']);
max_sys_time = max([[sensorData.sysTime] imgTime']);
sys_time_val = unique([[sensorData.sysTime] imgTime']);

% Print information about the data acquired
fprintf('\nData collection: %s\n',collectionName);
fprintf('Data time: %2.2f [secs]\n',max_sys_time-min_sys_time);
for it_sensor = 1:length(sensorData)
    fprintf('\n  %s: %6d reads (%d values), %3.3f Hz\n',sensorData(it_sensor).datatype,...
        length(sensorData(it_sensor).sysTime),...
        size(sensorData(it_sensor).values,2),...
        length(sensorData(it_sensor).sysTime)/...
        (sensorData(it_sensor).sysTime(end)-sensorData(it_sensor).sysTime(1)));
end

img = imread(sprintf('%s/%s/images_%s/%s',datasetFolder,...
    collectionName,imgres,listOfImg(it_img).name));
fprintf('\n  IMAGES: %6d images, %3.3f Hz\n',length(listOfImg),...
    length(imgTime)/(imgTime(end)-imgTime(1)));
fprintf('                  %6d x %4d %1d ch\n',size(img,2),size(img,1),...
    size(img,3));