package weather.provider;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import weather.model.Forecast;
import weather.model.Request;
import weather.model.RequestRule;
import weather.model.enumeration.FeatureType;
import weather.model.weather.enumeration.Overcast;
import weather.model.weather.enumeration.PeriodOfDay;
import weather.model.weather.enumeration.Phenomen;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component("gismeteo.by")
public class GismeteoRUProvider implements WeatherForecastProvider {

    @Override
    public List<Request> getWeather(RequestRule rule) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Date().getTime());
        List<Request> weeksRequests = parseDaysWeather(calendar, rule, null);
        if(weeksRequests != null){
            weeksRequests.addAll(parseDaysWeather(calendar, rule, "3-5-days/"));
        }
        return weeksRequests;
    }

    private List<Request> parseDaysWeather(Calendar calendar, RequestRule rule, String additionalURL){
        List<Request> weeksRequests;
        String tbDaily = "tbwdaily%s";
        try {
            Connection connection = Jsoup.connect(rule.getRequestLink() +
                    (additionalURL == null ? "" : additionalURL));
            Document doc = connection.get();
            weeksRequests = new ArrayList<Request>();
            int startTab = additionalURL == null ? 1 : 2;
            Request dayRequest = new Request();
            for(int i = startTab; i < 4; i++){
                Element forecastWeather = doc.getElementById(String.format(tbDaily, Integer.toString(i)));
                Elements forecastRows = forecastWeather.getElementsByTag("tr");
                int rowNumber = 0;

                for (; rowNumber < forecastRows.size(); rowNumber++) {
                    Elements forecastCells = forecastRows.get(rowNumber). children();
                    String periodOfDayString = forecastCells.get(0).text().trim().toLowerCase();
                    PeriodOfDay periodOfDay = null;
                    if (periodOfDayString != null) {
                        if (periodOfDayString.equals("день"))
                            periodOfDay = PeriodOfDay.DAY;
                        else if (periodOfDayString.equals("утро"))
                            periodOfDay = PeriodOfDay.MORNING;
                        else if (periodOfDayString.equals("вечер"))
                            periodOfDay = PeriodOfDay.EVENING;
                        else if (periodOfDayString.equals("ночь"))
                            periodOfDay = PeriodOfDay.NIGHT;
                    }
                    if (periodOfDay != null && (periodOfDay.equals(PeriodOfDay.DAY) || periodOfDay.equals(PeriodOfDay.NIGHT))) {
                            weeksRequests.add(parseForecastWeather(calendar.getTime(), periodOfDay, forecastCells, rule, dayRequest));
                    }
                }
                calendar.add(Calendar.DATE, 1);
                dayRequest = new Request();
            }
        } catch (Exception e) {
            weeksRequests = null;
        }
        return weeksRequests;

    }
    private Request parseForecastWeather(Date weatherDay, PeriodOfDay periodOfDay, Elements forecastCells, RequestRule rule, Request dayRequest) {

        String temperatureStr = forecastCells.get(3).child(0).text();
        int temperature = Integer.parseInt(temperatureStr.replace("+", "").replace("−", "-"));
        String overcastPhenomenasString = forecastCells.get(2).text();
        String overcast = null;
        String phenomens = null;
        if(overcastPhenomenasString.contains(",")){
            overcast = overcastDecode(overcastPhenomenasString.substring(0, overcastPhenomenasString.indexOf(",")));
            phenomens = phenomensDecode(overcastPhenomenasString.substring(overcastPhenomenasString.indexOf(",") + 1).trim().toLowerCase());
        } else {
            overcast = overcastDecode(overcastPhenomenasString.trim());
        }

        Forecast temperatureForecast = new Forecast();
        temperatureForecast.setRequest(dayRequest);
        FeatureType featuretype;
        if(periodOfDay.equals(PeriodOfDay.DAY)){
            featuretype = FeatureType.TEMPERATURE_DAY;
        } else{
            featuretype = FeatureType.TEMPERATURE_NIGHT;
        }
        temperatureForecast.setFeatureType(featuretype);
        temperatureForecast.setValue(Integer.toString(temperature));
        dayRequest.getForecasts().put(featuretype, temperatureForecast);

        if(periodOfDay.equals(PeriodOfDay.DAY)){
            featuretype = FeatureType.OVERCAST_DAY;
        } else{
            featuretype = FeatureType.OVERCAST_NIGHT;
        }
        Forecast overcastForecast = new Forecast();
        overcastForecast.setRequest(dayRequest);
        overcastForecast.setFeatureType(featuretype);
        overcastForecast.setValue(overcast);
        dayRequest.getForecasts().put(featuretype, overcastForecast);

        if(periodOfDay.equals(PeriodOfDay.DAY)){
            featuretype = FeatureType.PHENOMENA_DAY;
        } else{
            featuretype = FeatureType.PHENOMENA_NIGHT;
        }
        Forecast phenomenaForecast = new Forecast();
        phenomenaForecast.setRequest(dayRequest);
        phenomenaForecast.setFeatureType(featuretype);
        phenomenaForecast.setValue(phenomens);
        dayRequest.getForecasts().put(featuretype, phenomenaForecast);
        dayRequest.setForecastDate(weatherDay);
        dayRequest.setRequestDate(new Date());
        dayRequest.setRequestRule(rule);
        return dayRequest;

    }

    private String phenomensDecode(String phenomens) {
        String phenomensStr = null;

        if (phenomens != null && phenomens.trim().length() > 0) {
            String[] phenomensArray = null;
            if (phenomens.contains("."))
                phenomensArray = phenomens.split("\\.");
            else
                phenomensArray = phenomens.split(",");

            if (phenomensArray.length != 0) {
                StringBuilder phenomensBuilder = new StringBuilder();
                for (int i = 0; i < phenomensArray.length; i++) {
                    phenomensBuilder.append(decodePhenomen(phenomensArray[i]));
                    if(i < (phenomensArray.length - 1)){
                        phenomensBuilder.append(",");
                    }
                }
                phenomensStr = phenomensBuilder.toString();
            }
        }
        return phenomensStr;
    }

    private String decodePhenomen(String phenomenStr) {
        String phenomen = null;
        if (phenomenStr.contains("туман"))
            phenomen = Phenomen.FG.name();
        else if (phenomenStr.contains("дымка"))
            phenomen = Phenomen.BR.name();
        else if (phenomenStr.contains("ливневый снег"))
            phenomen = Phenomen.SHSN.name();
        else if (phenomenStr.contains("мокрый снег"))
            phenomen = Phenomen.SNRA.name();
        else if (phenomenStr.contains("снег"))
            phenomen = Phenomen.SN.name();
        else if (phenomenStr.contains("дождь"))
            phenomen = Phenomen.RA.name();
        else if (phenomenStr.contains("метель"))
            phenomen = Phenomen.BLSN.name();
        return phenomen;
    }

    private String overcastDecode(String overcast) {
        if (overcast.equals("Пасмурно")) {
            return Overcast.OVC.name();
        } else if (overcast.equals("Облачно с прояснениями")) {
            return Overcast.BKN.name();
        } else if (overcast.equals("Облачно")) {
            return Overcast.SCT.name();
        } else if (overcast.equals("Небольшая облачность")) {
            return Overcast.FEW.name();
        } else if (overcast.equals("Малооблачно")) {
            return Overcast.NSC.name();
        } else if (overcast.equals("Ясно")) {
            return Overcast.SKC.name();
        } else {
            return Overcast.NONE.name();
        }
    }
}
