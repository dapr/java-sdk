/*
 * Copyright 2025 The Dapr Authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
limitations under the License.
*/

package io.dapr.springboot4.examples.producer;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomerStore {
  private Map<String, Customer> customers = new HashMap<>();

  public void addCustomer(Customer customer) {
    customers.put(customer.getCustomerName(), customer);
  }

  public Customer getCustomer(String customerName) {
    return customers.get(customerName);
  }

  public Collection<Customer> getCustomers() {
    return customers.values();
  }

}
