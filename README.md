# StreamCart

StreamCart is a **Full-Fledged E-Commerce Platform with Real-Time Analytics**.

### Key Features
**1) Product Catelog with Categories:**
- The platform must support a variety of product categories such as Electronics, Apparel, Books, and Home Appliances.
- Each product will have attributes specific to its category (e.g., electronics will have technical specs, apparel will have sizes and colors).
- Users can filter and search products based on category-specific attributes.

[**2) User Authentication and Profiles:**](./user-service/README.md)
- Implement user registration and authentication.
- Users can manage profiles, view order history, and save favorite products across categories.

[**3) Shopping Cart and Checkout:**](./cart-service/README.md)
- Users can add items to a shopping cart across multiple sessions.
- Implement a checkout process with support for multiple payment gateways.
- Include validation for product availability before order placement.

**4) Order Management and Payment:**
- After successful payment, the order should be finalized, and the inventory should be updated. 
- If the payment fails or times out, the system should roll back the order and restore item quantities.

**5) Inventory Management:**
- Real-time inventory tracking for products in different categories.
- Handle out-of-stock scenarios gracefully and notify users when products are restocked.
- Implement a dynamic pricing feature based on stock levels or user demand.

**6) Real-Time Analytics and Recommendations:**
- Track user interactions like page views, product searches, and purchases in real-time.
- Process streams of user interactions and provide real-time insights:
    - Identify popular products and categories.
    - Detect abandoned carts and send reminders to users.
    - Implement a recommendation system that suggests products based on browsing history.
- Display real-time sales and product performance dashboards.

