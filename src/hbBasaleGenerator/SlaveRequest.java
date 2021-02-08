package hbBasaleGenerator;

import java.util.ArrayList;
import java.util.List;

public class SlaveRequest {
    String calval_id;
    String site;
    String sensor_mode;
    String requested_level;

    public String getCalval_id() {
        return calval_id;
    }

    public void setCalval_id(String calval_id) {
        this.calval_id = calval_id;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSensor_mode() {
        return sensor_mode;
    }

    public void setSensor_mode(String sensor_mode) {
        this.sensor_mode = sensor_mode;
    }

    public String getRequested_level() {
        return requested_level;
    }

    public void setRequested_level(String requested_level) {
        this.requested_level = requested_level;
    }
}
