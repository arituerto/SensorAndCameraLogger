function sensor_read_current = getLastRead(sensor_read,...
    current_time)
        
    if strcmp(sensor_read.datatype(1:4), 'CPRO')
        time = sensor_read.sysTime;
    else
        time = sensor_read.evntTime;
    end
    
    k = find(time <= current_time);
    
    if ~isempty(k)
        sensor_read_current.datatype = sensor_read.datatype;
        sensor_read_current.sysTime = sensor_read.sysTime(k(end));
        sensor_read_current.evntTime = sensor_read.evntTime(k(end));
        sensor_read_current.value = sensor_read.value(k(end),:);
    else
        sensor_read_current.datatype = sensor_read.datatype;
        sensor_read_current.sysTime = [];
        sensor_read_current.evntTime = [];
        sensor_read_current.value = [];
    end
end