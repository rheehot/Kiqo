package com.thirstygoat.kiqo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import com.thirstygoat.kiqo.model.Item;
import com.thirstygoat.kiqo.model.Person;

/**
 * Created by bradley on 9/04/15.
 */
public class Utilities {

    public static String concatenatePeopleList(List<Person> people, int max) {
        String list = "";
        for (int i = 0; i < Math.min(people.size(), max)-1; i++) {
            list += people.get(i).getShortName() + ", ";
        }
        list += people.get(Math.min(people.size(), max)-1).getShortName();
        if (Math.min(people.size(), max) < people.size()) {
            final int remaining = people.size() - max;
            final String others = (remaining % 2 == 0) ? "others" : "other";
            list += " and " + remaining + " " + others;
        }

        return list;
    }

    public static String commaSeparatedValues(List<? extends Item> list) {
        String concatenatedString = "";

        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                concatenatedString += list.get(i).getShortName();
                if (i != list.size() - 1) {
                    concatenatedString += ", ";
                }
            }
        }

        return concatenatedString;
    }

    public static StringProperty commaSeparatedValuesProperty(ObservableList<? extends Item> list) {
        StringProperty result = new SimpleStringProperty();
        result.set(commaSeparatedValues(list));

        // Add listeners on each of the string properties within the observable list
        final ChangeListener<String> stringChangeListener =
                (observable, oldValue, newValue) -> result.set(commaSeparatedValues(list));

        for (Item item : list) {
            item.shortNameProperty().addListener(stringChangeListener);
        }

        list.addListener((ListChangeListener<Item>) c -> {
            c.next();
            // Remove shortNameProperty listeners on each of items removed from the list
            for (Item item : c.getRemoved()) {
                item.shortNameProperty().removeListener(stringChangeListener);
            }

            // Add shortNameProperty listener for each of the items added to the list
            for (Item item : c.getAddedSubList()) {
                item.shortNameProperty().addListener(stringChangeListener);
            }

            result.set(commaSeparatedValues(list));
        });

        return result;
    }

    /**
     * Checks whether the given shortname is unique among the given Collection.
     * @param shortName Short Name to be checked
     * @param items items among which the name must be unique
     * @return shortName is unique among items
     */
    public static boolean shortnameIsUnique(String shortName, Collection<? extends Item> items) {
        // copy all the shortnames into a new list
        final Collection<String> list = new ArrayList<>();
        list.addAll(items.stream().map(Item::getShortName).collect(Collectors.toList()));

        // now for the actual check
        return !list.contains(shortName);
    }
}
