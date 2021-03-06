package weather.util;


import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import weather.dto.RequestDto;
import weather.model.Forecast;
import weather.model.Request;

import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {

    public static Date formatStartDate(Date date) {
        return DateUtils.truncate(date, Calendar.DATE);
    }

    public static Date formatEndDate(Date date) {
        return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
    }

    public static List<RequestDto> requestConverter(List<Request> requests) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE", Locale.getDefault());
        List<RequestDto> requestsDto = new ArrayList<RequestDto>(requests.size());
        for (Request request : requests) {
            RequestDto requestDto = new RequestDto();
            requestDto.setForecastDate(request.getForecastDate());
            requestDto.setRequestDate(request.getRequestDate());
            requestDto.setForecastDayOfWeek(simpleDateFormat.format(request.getForecastDate()));
            requestDto.setRequestDayOfWeek(simpleDateFormat.format(request.getRequestDate()));
            requestDto.setForecasts(request.getForecasts());
            requestDto.setRequestRule(request.getRequestRule());
            requestsDto.add(requestDto);
        }
        return requestsDto;
    }

    /*
    * Compare rated with real
    * */
    public static Integer calcTempRate(String ratedValue, String realValue) {
        try {
            int ratedValueInt = Integer.parseInt(ratedValue);
            int realValueInt = Integer.parseInt(realValue);

            int difference = Math.abs(realValueInt - ratedValueInt);

            switch (difference) {
                case 0 : return 100;
                case 1 : return 90;
                case 2 : return 80;
                case 3 : return 70;
                case 4 : return 60;
                case 5 : return 50;
                case 6 : return 40;
                case 7 : return 30;
                case 8 : return 20;
                case 9 : return 10;
                default : return 0;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();

            if (StringUtils.isEmpty(ratedValue) || StringUtils.isEmpty(realValue) )
                return null;

            if (ratedValue.equals(realValue))
                return 100;
        }

        return 50;
    }
}


