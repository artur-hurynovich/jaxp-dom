package jaxp.dom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.function.Consumer;
class Cars {
    private ArrayList<Car> cars;
    Cars() {
        cars = new ArrayList<>();
    }
    void add(Car car) {
        cars.add(car);
    }
    void forEach(Consumer<? super Car> consumer) {
        cars.forEach(consumer);
    }
    @Override
    public String toString() {
        return cars.toString();
    }
}
class Car {
    private String mark;
    private String model;
    private TechChar techChar;
    public static class TechChar {
        private XMLGregorianCalendar date;
        private double engineCapacity;
        private String engineType;
        void setDate(LocalDate date) {
            try {
                this.date = DatatypeFactory.newInstance().newXMLGregorianCalendar(date.toString());
            }
            catch (DatatypeConfigurationException e) {
                e.printStackTrace();
            }
        }
        void setEngineCapacity(double engineCapacity) {
            this.engineCapacity = engineCapacity;
        }
        void setEngineType(String engineType) {
            this.engineType = engineType;
        }
        XMLGregorianCalendar getDate() {
            return date;
        }
        double getEngineCapacity() {
            return engineCapacity;
        }
        String getEngineType() {
            return engineType;
        }
        @Override
        public String toString() {
            return date.toGregorianCalendar().toZonedDateTime().toLocalDate().
                format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + ", " + 
                engineCapacity + " (" + engineType + ")";
        }
    }
    void setMark(String mark) {
        this.mark = mark;
    }
    void setModel(String model) {
        this.model = model;
    }
    void setTechChar(TechChar techChar) {
        this.techChar = techChar;
    }
    String getMark() {
        return mark;
    }
    String getModel() {
        return model;
    }
    TechChar getTechChar() {
        if (techChar == null) {
            return new TechChar();
        }
        else {
            return techChar;
        }
    }
    @Override
    public String toString() {
        return mark + " " + model + ": " + techChar.toString();
    }
}
class CarBuilder {
    static Car buildCar(String mark, String model, LocalDate date, double engineCapacity, String engineType) {
        Car car = new Car();
        car.setMark(mark);
        car.setModel(model);
        Car.TechChar techChar = car.getTechChar();
        techChar.setDate(date);
        techChar.setEngineCapacity(engineCapacity);
        techChar.setEngineType(engineType);
        car.setTechChar(techChar);
        return car;
    }
    static Car buildCar(Element elementCar) {
        String mark = elementCar.getChildNodes().item(0).getTextContent();
        String model = elementCar.getChildNodes().item(1).getTextContent();
        NodeList characteristics = elementCar.getChildNodes().item(2).getChildNodes();
        char[] dateChars = characteristics.item(0).getTextContent().toCharArray();
        String year = new StringBuilder().append(dateChars[0]).append(dateChars[1]).append(dateChars[2]).a
            ppend(dateChars[3]).toString();
        String month = new StringBuilder().append(dateChars[5]).append(dateChars[6]).toString();
        String day = new StringBuilder().append(dateChars[8]).append(dateChars[9]).toString();
        LocalDate date = LocalDate.of(Integer.valueOf(year), Integer.valueOf(month), Integer.valueOf(day));
        double engineCapacity = Double.valueOf(characteristics.item(1).getTextContent());
        String engineType = characteristics.item(2).getTextContent();
        return buildCar(mark, model, date, engineCapacity, engineType);
    }
}
class CarElementBuilder {
    static Element buildCarElement(Car car, Document document) {
        Element elementCar = document.createElement("car");
        Element elementMark = document.createElement("mark");
        elementMark.setTextContent(car.getMark());
        Element elementModel = document.createElement("model");
        elementModel.setTextContent(car.getModel());
        Element elementCharacteristics = document.createElement("characteristics");
        Element elementDate = document.createElement("date");
        elementDate.setTextContent(car.getTechChar().getDate().toString());
        Element elementEngineCapacity = document.createElement("engineCapacity");
        elementEngineCapacity.setTextContent(String.valueOf(car.getTechChar().getEngineCapacity()));
        Element elementEngineType = document.createElement("engineType");
        elementEngineType.setTextContent(car.getTechChar().getEngineType());
        elementCar.appendChild(elementMark);
        elementCar.appendChild(elementModel);
        elementCharacteristics.appendChild(elementDate);
        elementCharacteristics.appendChild(elementEngineCapacity);
        elementCharacteristics.appendChild(elementEngineType);
        elementCar.appendChild(elementCharacteristics);
        return elementCar;
    }
}
public class DomClass {
    public static void main(String[] args) {
        File xmlFile = new File("cars.xml");
        Cars cars = new Cars();
        cars.add(CarBuilder.buildCar("Audi", "Q7", LocalDate.of(2016, 5, 10),
                3.0, "Diesel"));
        cars.add(CarBuilder.buildCar("BMW", "X5", LocalDate.of(2010, 7, 4),
                4.4, "Gasoline"));
        cars.add(CarBuilder.buildCar("Porsche", "911", LocalDate.of(2005, 1, 12),
                5.0, "Gasoline"));
        cars.add(CarBuilder.buildCar("Volkswagen", "Passat", LocalDate.of(1989, 9, 2),
                1.6, "Diesel"));
        try (PrintWriter writer = new PrintWriter(xmlFile)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element carsElement = document.createElement("cars");
            cars.forEach(car -> carsElement.appendChild(CarElementBuilder.buildCarElement(car, document)));
            document.appendChild(carsElement);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            Cars newCars = new Cars();
            Document newDocument = builder.parse(xmlFile);
            Element newCarsElement = newDocument.getDocumentElement();
            NodeList carsList = newCarsElement.getChildNodes();
            for (int i = 0; i < carsList.getLength(); i++) {
                newCars.add(CarBuilder.buildCar((Element) carsList.item(i)));
            }
            newCars.forEach(System.out::println);
        }
        catch (ParserConfigurationException|IOException|TransformerException|SAXException e) {
            e.printStackTrace();
        }
    }
}
