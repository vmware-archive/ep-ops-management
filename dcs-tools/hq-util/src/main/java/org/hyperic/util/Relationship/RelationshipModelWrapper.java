package org.hyperic.util.Relationship;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class wraps a Map<String, Collection<String>> to make it simple to serialize and deserialize when used as a
 * relationship model.
 * <p/>
 * Created by bharel on 3/29/2015.
 */
public class RelationshipModelWrapper implements Map<String, Collection<String>> {
    private static final Log log = LogFactory.getLog(RelationshipModelWrapper.class);

    private static final String listSeparator = ",";
    private final String lineSeparator = "\n";
    private final String keyValueSeparator = "=";
    private Map<String, Collection<String>> innerMap;

    public RelationshipModelWrapper() {
        innerMap = new HashMap<String, Collection<String>>();
    }

    /**
     * Ctor from String. Example input: test1=a,b,c test2=a\,,b test3=a\,,b=,=c=,\,d\,\,,==e==
     * 
     * @param relationshipModel
     */
    public RelationshipModelWrapper(String relationshipModel) {
        innerMap = new HashMap<String, Collection<String>>();

        for (String entry : relationshipModel.split(lineSeparator)) {
            String[] keyAndValue = entry.split(keyValueSeparator, 2);
            String key = keyAndValue[0];
            Collection<String> value = getStringCollection(keyAndValue[1]);
            innerMap.put(key, value);
        }
    }

    private Collection<String> getStringCollection(String separatedStrings) {
        List<String> matchList = getSeparatedStrings(separatedStrings);

        List<String> result = new ArrayList<String>();
        for (String match : matchList) {
            if (StringUtils.isBlank(match)) {
                continue;
            }

            result.add(match.replaceAll(Pattern.quote(String.format("\\%s", listSeparator)), listSeparator));
        }

        return result;
    }

    private List<String> getSeparatedStrings(String separatedStrings) {
        List<String> matchList = new ArrayList<String>();
        /*
        "(?:         # Start of group\n"
        " \\\\.      # Match either an escaped character\n" +
        "|           # or\n" +
        "[^\\\\,]++  # Match one or more characters except comma/backslash\n",
        ")*          # Do this any number of times",
        */
        Pattern regex = Pattern.compile(String.format("(?:\\\\.|[^\\\\%s]++)*", listSeparator));
        Matcher regexMatcher = regex.matcher(separatedStrings);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }
        return matchList;
    }

    public void put(String key,
                    String value) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
            return;
        }

        log.debug(String.format("'%s'='%s'", key, value));

        Collection<String> strings = get(key);
        if (strings == null) {
            innerMap.put(key, Arrays.asList(value));
            return;
        }

        innerMap.get(key).add(value);
    }

    public Map<String, Collection<String>> getInnerMap() {
        return innerMap;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Entry<String, Collection<String>> stringCollectionEntry : innerMap.entrySet()) {
            stringBuilder.append(stringCollectionEntry.getKey());
            stringBuilder.append(keyValueSeparator);
            stringBuilder.append(getEscapedValues(stringCollectionEntry.getValue()));
            stringBuilder.append(lineSeparator);
        }

        return stringBuilder.toString();
    }

    private String getEscapedValues(Collection<String> strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String string : strings) {
            stringBuilder.append(string.replace(listSeparator, '\\' + listSeparator));
            stringBuilder.append(listSeparator);
        }

        stringBuilder.setLength(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    public void setInnerMap(Map<String, Collection<String>> innerMap) {
        this.innerMap = innerMap;
    }

    public int size() {
        return innerMap.size();
    }

    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return innerMap.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return innerMap.containsValue(value);
    }

    public Collection<String> get(Object key) {
        return innerMap.get(key);
    }

    public Collection<String> put(String key,
                                  Collection<String> values) {
        for (String value : values) {
            put(key, value);
        }

        return null;
    }

    public Collection<String> remove(Object key) {
        return innerMap.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Collection<String>> map) {
        log.debug(String.format("adding: %s", map));
        innerMap.putAll(map);
    }

    public void clear() {
        innerMap.clear();
    }

    public Set<String> keySet() {
        return innerMap.keySet();
    }

    public Collection<Collection<String>> values() {
        return innerMap.values();
    }

    public Set<Entry<String, Collection<String>>> entrySet() {
        return innerMap.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return innerMap.equals(o);
    }

    @Override
    public int hashCode() {
        return innerMap.hashCode();
    }
}
