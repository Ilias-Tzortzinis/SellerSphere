# StreamCart

StreamCart is a **Fully-Fledged E-Commerce Platform with realtime analytics**. 
This is a personal project to hone my skills and knowledge of different technlogies
in the programming world.

**The Architecture is documented in [<u>arch.md</u>](./docs/arch.md).**
## Features 

- ### User Management: 
    - User registration and authentication (sign up, log in, password recovery).
    - User profile management (view and edit personal information).
- ### Shopping Cart:
    - Add, update, and remove products from the shopping cart.
    - View cart contents and total price.
- ### Product Management:
    - CRUD (Create, Read, Update, Delete) operations for products.
    - Categorization of products (e.g., electronics, clothing, etc.).
    - Product search and filtering capabilities.
- ### Order Management
    - Checkout process (confirm items, enter shipping information, and payment).
    - Order history view for users.
- ### Payment Integration
    - Integrate a payment gateway like Stripe.
- ### Recommendation Engine
    - Use user-item interaction data to generate product recommendations.

## Roles
- ### Admin
    - Control user accounts (activate, deactivate).
    - Manage seller accounts (verify, activate, deactivate)
    - Monitor sales and order statistics.
- ### Seller
    - Create and manage their own product listings.
    - View and manage orders specific to their products.
    - Track inventory levels and manage stock.
- ### User (Customer)
    - Register and manage their account profile.
    - Browse and search for products.
    - Add products to the shopping cart and place orders.
    - View order history and track order status.
    - Receive personalized recommendations based on behavior.
- ### Guest
    - Browse and search for products without logging in.
    - Add products to the shopping cart (with limited functionality).
    - Prompted to register or log in during checkout.