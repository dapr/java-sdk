package io.dapr.it.methodinvoke.http;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SpringBoot Controller to handle input binding.
 */
@RestController
public class MethodInvokeController {

    private static final Map<Integer,String> messagesReceived = new HashMap<>();
    private static final List<Person> persons= new ArrayList<>();

    @PostMapping("/messages")
    public void postMessages(@RequestBody String message){
        System.out.println("Controller got message: " + message);
        final Optional<Integer> maxKey = messagesReceived.keySet().stream().max(Integer::compareTo);
        final Integer key = maxKey.orElse(-1)+1;
        messagesReceived.put(key,message);
        System.out.println("Controller save the message: " + message);
    }

    @PutMapping(path = "/messages/{messageId}")
    public void putMessages(@PathVariable("messageId") Integer messageId, @RequestBody String message){
        messagesReceived.put(messageId,message);
    }

    @DeleteMapping(path = "/messages/{messageId}")
    public void deleteMessages(@PathVariable("messageId") Integer messageId){
        messagesReceived.remove(messageId);
    }

    @GetMapping(path = "/messages")
    public Map<Integer, String> getMessages() {
        return messagesReceived;
    }

    @PostMapping("/persons")
    public void postPerson(@RequestBody Person person){
        System.out.println("Controller get person: " + person);
        final Optional<Integer> max = persons.stream().map(person1 -> person1.getId()).max(Integer::compareTo);
        final Integer key = max.orElse(-1)+1;
        person.setId(key);
        persons.add(person);
        System.out.println("Controller save the person: " + person);
    }

    @PutMapping(path = "/persons/{personId}")
    public void putPerson(@PathVariable("personId") Integer personId, @RequestBody Person person){
        final Optional<Person> auxPerson = persons.stream().filter(person1 -> person1.getId() == personId).findFirst();
        if(auxPerson.isPresent()){
            auxPerson.get().setName(person.getName());
            auxPerson.get().setLastName(person.getLastName());
            auxPerson.get().setBirthDate(person.getBirthDate());
        }
    }

    @DeleteMapping(path = "/persons/{personId}")
    public void deletePerson(@PathVariable("personId") Integer personId){
        final Optional<Person> auxPerson = persons.stream().filter(person1 -> person1.getId() == personId).findFirst();
        if(auxPerson.isPresent()) {
            persons.remove(auxPerson.get());
        }
    }

    @GetMapping(path = "/persons")
    public List<Person> getPersons() {
        return persons;
    }

    @PostMapping(path = "/sleep")
    public void sleep(@RequestBody int seconds) throws InterruptedException {
        if (seconds < 0) {
            throw new IllegalArgumentException("Sleep time cannot be negative.");
        }
        Thread.sleep(seconds * 1000);
    }

    @GetMapping(path = "/health")
    public void health() {
    }
}
