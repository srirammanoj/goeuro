package goeuro.service;

import goeuro.util.BusRouteDataFile;
import goeuro.util.DirectBusRouteResult;
import goeuro.validator.StationId;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Main service handler for the api
 * Created by manoj on 19/09/16.
 */
@Service
@Path("direct")
public class DirectBusRouteService {

    private static final Logger logger = LoggerFactory.getLogger(DirectBusRouteService.class);

    /**
     * Autowired class for loading the data file.
     */
    private final BusRouteDataFile busRouteDataFile;

    @Autowired
    public DirectBusRouteService(BusRouteDataFile busRouteDataFile) {
        this.busRouteDataFile = busRouteDataFile;
    }

    /**
     * @param depSid departure Station Id (Mandatory Parameter)
     * @param arrSid arrival Station Id (Mandatory Parameter)
     * @return JSON representation of DirectBusResult Object.
     */
    @GET
    @Produces("application/json")
    public DirectBusRouteResult getDirectBusRoute(@QueryParam("dep_sid") @NotEmpty @StationId String depSid,
                                                  @QueryParam("arr_sid") @NotEmpty @StationId String arrSid,
                                                  @Context final HttpServletResponse response) {

        int departureStationId = Integer.parseInt(depSid);
        int arrivalStationId = Integer.parseInt(arrSid);

        HashMap<Integer, HashSet<Integer>> busRouteData;
        busRouteData = busRouteDataFile.getBusRouteData();

        if (busRouteData.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.flushBuffer();
            } catch (IOException e) {
                logger.error("Error setting response status ", e);
            }
            return new DirectBusRouteResult(departureStationId, arrivalStationId, false);
        }

        HashSet<Integer> depSidRouteSet = busRouteData.get(departureStationId);
        HashSet<Integer> arrSidRouteSet = busRouteData.get(arrivalStationId);

        boolean directBusRoute = intersection(depSidRouteSet, arrSidRouteSet);

        return new DirectBusRouteResult(departureStationId, arrivalStationId, directBusRoute);
    }

    /**
     * @param depSidRouteSet The set of route Id's servicing the departure station
     * @param arrSidRouteSet The set of route Id's servicing the arrival station
     * @return 'true' if any route services both the departure and arrival stations
     *          ( or if departure and arrival stations are the same ), 'false' otherwise.
     */
    private boolean intersection(HashSet<Integer> depSidRouteSet, HashSet<Integer> arrSidRouteSet) {

        if (depSidRouteSet == null || arrSidRouteSet == null) {
            return false;
        }

        for (int depSidRoute : depSidRouteSet) {
            // If atleast one route has both the station id's we return true
            if (arrSidRouteSet.contains(depSidRoute)) {
                return true;
            }
        }
        return false;
    }

}
