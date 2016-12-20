function showCurrentSensorReads(fig,...
    datasetFolder, collectionName, imgres,...
    sensor_reads_current,...
    time_current)

% fprintf('TIME: %4.4f\n', time_current);

for (it_sensor = 1:length(sensor_reads_current))
    
    datatype = sensor_reads_current(it_sensor).datatype;
    value = sensor_reads_current(it_sensor).value;
    if (~isempty(value))
        switch datatype
            case 'CAMERA'
                img = imread(sprintf('%s/%s/images_%s/%s',datasetFolder,...
                    collectionName,imgres,value{1}));
                figure(fig);
                imshow(img);
        end
    end
end