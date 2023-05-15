package com.bookstore.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bookstore.dto.AddressDto;
import com.bookstore.dto.LoginDto;
import com.bookstore.dto.OrderDto;
import com.bookstore.entities.BookModel;
import com.bookstore.entities.CartBook;
import com.bookstore.entities.OrderModel;
import com.bookstore.entities.UserModel;
import com.bookstore.exception.UserExceptions;
import com.bookstore.mailcontroller.EmailSenderService;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.CartRepository;
import com.bookstore.repository.OrderRepository;
import com.bookstore.repository.UserRepository;
import com.bookstore.utility.UserToken;

@Service
public class OrderService implements  IOrderService{
    @Autowired
    IBookService bookService;
    @Autowired
    IUserService userService;

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    CartRepository cartRepository;
    @Autowired
	ModelMapper modelmapper;
    @Autowired
	UserToken jwt;
    @Autowired
    EmailSenderService iEmailService;
    
    @Override
    public OrderModel placeOrder(OrderDto orderDto,String token){
    	LoginDto email = jwt.decode(token);
		Optional<UserModel> user = userRepository.findByEmail(email.getEmail());
        Optional<BookModel> book = bookRepository.findById(orderDto.bookId);
        float totalPrice = book.get().getPrice();
        if (user.isPresent() && book.isPresent()) {
        	if (user.get().isLogin() == false) {
    			throw new UserExceptions(user.get().getRole()+" Not Logged IN.");
    		}
        	if(book.get().getQuantity()==0) {
        		throw new UserExceptions("Book sold out! try later!");
        	}
            book.get().setQuantity(book.get().getQuantity()-1);
            bookRepository.save(book.get());
            OrderModel order = new OrderModel(user.get().getCartId(),book.get(),totalPrice,1,orderDto.address);
            orderRepository.save(order);
            iEmailService.sendEmail(user.get().getEmail(), "Order Info..", "Order Placed successfully & your OrderId is : "+order.getOrderId());
            return order;
        } else {
            throw new UserExceptions("User id or book id did not match! Please check and try again!");
        }
    }
    @Override
	public List<CartBook> placeOrderByCart(String token,AddressDto address) {
    	LoginDto email = jwt.decode(token);
		Optional<UserModel> userModel = userRepository.findByEmail(email.getEmail());
		List<CartBook> cartBook=cartRepository.findAllByUserCartId(userModel.get().getCartId().getCartId());
		if (userModel.get().isLogin() == false) {
			throw new UserExceptions(userModel.get().getRole()+" Not Logged IN.");
		}
		if(cartBook.isEmpty()) {
			throw new UserExceptions("Cart is Empty.");
		}else {
			for(CartBook cart:cartBook) {
				OrderModel order = modelmapper.map(cart, OrderModel.class);
				Optional<BookModel> book = bookRepository.findById(order.getBookId().getBookId());
				book.get().setQuantity(book.get().getQuantity()-order.getQuantity());
				bookRepository.save(book.get());
				order.setAddress(address.getAddress());
				orderRepository.save(order);
				cartRepository.deleteById(cart.getId());
				iEmailService.sendEmail(userModel.get().getEmail(), "Order Info..", "Order Placed successfully & your OrderId is : "+order.getOrderId());
			}
			
		}
		return cartBook;
	}
    @Override
    public List<OrderModel> getAllOrders(String token){
    	LoginDto email = jwt.decode(token);
		Optional<UserModel> userModel = userRepository.findByEmail(email.getEmail());
        List<OrderModel> orderModelList = orderRepository.findAll();
        if (userModel.get().isLogin() == false) {
			throw new UserExceptions(userModel.get().getRole()+" Not Logged IN.");
		}
        return orderModelList;
    }
    @Override
    public OrderModel getOrderById(long orderId,String token){
    	LoginDto email = jwt.decode(token);
		Optional<UserModel> userModel = userRepository.findByEmail(email.getEmail());
        Optional<OrderModel> orderModel = orderRepository.findById(orderId);
        if (userModel.get().isLogin() == false) {
			throw new UserExceptions(userModel.get().getRole()+" Not Logged IN.");
		}
        if(orderModel.get().isCancelled()){
        	throw new UserExceptions("Order was cancelled Earlier of OrderId: "+orderId);
        }
        if(orderModel.isPresent())
            return orderModel.get();
        else
            throw new UserExceptions("OrderId: "+orderId+" not found..");
    }
    @Override
    public OrderModel cancelOrderById(long orderId,String token) {
    	LoginDto email = jwt.decode(token);
		Optional<UserModel> user = userRepository.findByEmail(email.getEmail());
         Optional<OrderModel> orderModel = orderRepository.findById(orderId);
         Optional<BookModel> book = bookRepository.findById(orderModel.get().getBookId().getBookId());
         if (user.get().isLogin() == false) {
 			throw new UserExceptions(user.get().getRole()+" Not Logged IN.");
 		}
        if (orderModel.isPresent()) {
            book.get().setQuantity(book.get().getQuantity()+orderModel.get().getQuantity());
            bookRepository.save(book.get());
            OrderModel order = new OrderModel(user.get().getCartId(),book.get(),orderModel.get().getPrice(),orderModel.get().getQuantity(),orderModel.get().getAddress());
            order.setOrderId(orderModel.get().getOrderId());
            order.setOrderDate(orderModel.get().getOrderDate());
            order.setCancelled(true);
            order.setCancelledDate(LocalDate.now());
            iEmailService.sendEmail(user.get().getEmail(), "Order Cancelled successfully.", "Your Order of order no. : "+orderModel.get().getOrderId() +" Has Been succefully Cancelled.");
            return orderRepository.save(order);
        } else {
            throw new UserExceptions("User id or book id did not match! Please check and try again!");
        }
    }
    
}
