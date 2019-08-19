# retrofitcalladapter
Showing uses of retrofit custom call adapter to handle response and exceptions

Retrofit default Callback is generic. You need to check status code everytime to handle response or different types of exception
(authentication exception, client or server exception). This custom call adapter can be used to handle response and exception in 
single place so that if doesn't spread accross your application.

