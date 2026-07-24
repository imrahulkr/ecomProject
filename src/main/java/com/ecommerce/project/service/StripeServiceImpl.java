package com.ecommerce.project.service;

import com.ecommerce.project.payload.StripePaymentDTO;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeSearchResult;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.CustomerSearchParams;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret.key}")
    private String stripeApiKey;

    @Override
    public PaymentIntent paymentIntent(StripePaymentDTO stripePaymentDTO) throws StripeException {
        System.out.println("StripePaymentDTO: " + stripePaymentDTO);
        StripeClient client = new StripeClient(stripeApiKey);

        Customer customer;
        // Search for customer with email id.
        CustomerSearchParams customerSearchParams =
                CustomerSearchParams.builder()
                        .setQuery("email:'" + stripePaymentDTO.getEmail() + "'")
                        .build();
        StripeSearchResult<Customer> stripeSearchResult =
                client.v1().customers().search(customerSearchParams);
        if(stripeSearchResult.getData().isEmpty()) {
            // If customer does not exist, create new customer
            CustomerCreateParams customerCreateParams =
                    CustomerCreateParams.builder()
                            .setName(stripePaymentDTO.getName())
                            .setEmail(stripePaymentDTO.getEmail())
                            .setAddress(
                                    CustomerCreateParams.Address.builder()
                                            .setLine1(stripePaymentDTO.getAddress().getStreet())
                                            .setCity(stripePaymentDTO.getAddress().getCity())
                                            .setState(stripePaymentDTO.getAddress().getState())
                                            .setPostalCode(stripePaymentDTO.getAddress().getPincode())
                                            .setCountry(stripePaymentDTO.getAddress().getCountry())
                                            .build()
                            )
                            .build();
            customer = client.v1().customers().create(customerCreateParams);
        } else {
            // If customer Exist, Fetch existing customer (alredy fetched while searching )
//            customer = stripeSearchResult.getData().get(0);
            customer = stripeSearchResult.getData().getFirst();
        }


        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(stripePaymentDTO.getAmount())
                        .setCurrency(stripePaymentDTO.getCurrency())
                        .setCustomer(customer.getId())
                        .setDescription(stripePaymentDTO.getDescription())
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();
        return client.v1().paymentIntents().create(params);
//        PaymentIntent paymentIntent = client.v1().paymentIntents().create(params);
//        return paymentIntent;
    }
}
