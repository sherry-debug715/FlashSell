package com.flashsell.flashsell.web;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.flashsell.flashsell.db.dao.OrderDao;
import com.flashsell.flashsell.db.dao.SeckillActivityDao;
import com.flashsell.flashsell.db.dao.SeckillCommodityDao;
import com.flashsell.flashsell.db.po.Order;
import com.flashsell.flashsell.db.po.SeckillActivity;
import com.flashsell.flashsell.db.po.SeckillCommodity;
import com.flashsell.flashsell.services.SeckillActivityService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;


@Slf4j
@Controller
public class SeckillActivityController {
    
    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    SeckillActivityService seckillActivityService;

    @Autowired
    OrderDao orderDao;

    @RequestMapping("/seckills")
    public String activityList(Map<String, Object> resultMap) {
        List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
        resultMap.put("seckillActivities", seckillActivities);
        return "seckill_activity"; 
    }

    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(Map<String, Object> resultMap, @PathVariable long seckillActivityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());

        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);
        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice", seckillActivity.getOldPrice());
        resultMap.put("commodityId", seckillActivity.getCommodityId());
        resultMap.put("commodityName", seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());
        return "seckill_item"; // returns the view "seckill_item"
    }

    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap) throws ParseException {
        
            startTime = startTime.substring(0, 10) + startTime.substring(11);
            endTime = endTime.substring(0, 10) + endTime.substring(11);
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
            SeckillActivity seckillActivity = new SeckillActivity();
            seckillActivity.setName(name);
            seckillActivity.setCommodityId(commodityId);
            seckillActivity.setSeckillPrice(seckillPrice);
            seckillActivity.setOldPrice(oldPrice);
            seckillActivity.setTotalStock(seckillNumber);
            seckillActivity.setAvailableStock((int) seckillNumber); // Corrected Integer construction
            seckillActivity.setLockStock(0L);
            seckillActivity.setActivityStatus(1);
            seckillActivity.setStartTime(format.parse(startTime));
            seckillActivity.setEndTime(format.parse(endTime));
            seckillActivityDao.insertSeckillActivity(seckillActivity);
            resultMap.put("seckillActivity", seckillActivity);
            return "add_success"; // returns the view "add_success"
    }

    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity"; // returns the view "add_activity"
    }

    /**
    * Handle flashsale request
    * @param userId
    * @param seckillActivityId
    * @return
    */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable long userId, 
                                        @PathVariable long seckillActivityId) {
        boolean stockValidateResult = false;
        ModelAndView modelAndView = new ModelAndView();
        try {
        /*
        * Make sure flash sale can be processed 
        */
        stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        if (stockValidateResult) {
            Order order = seckillActivityService.createOrder(seckillActivityId, userId);
            modelAndView.addObject("resultInfo","Flash sale successful, order is being created, Order ID: "
            + order.getOrderNo());
            modelAndView.addObject("orderNo",order.getOrderNo());
        } else {
            modelAndView.addObject("resultInfo","Sorry, the selected product is out of stock");
        }
        } catch (Exception e) {
            log.error("Flashsale system error" + e.toString());
            modelAndView.addObject("resultInfo","Flashsale failed");
        }
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    /*
     * Order status checking
     * @param orderNo
     * @return
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(@PathVariable String orderNo) {
        log.info("Order Status, Order number: " + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();

        if (order != null) {
            log.info("Order found with status: " + order.getOrderStatus());

            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            log.info("Order not found, redirecting to order_wait page.");
            modelAndView.setViewName("order_wait");
        }
        return modelAndView;
    }

    /**
     * Order payment
     * @return
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(@PathVariable String orderNo){
        seckillActivityService.payOrderProcess(orderNo);
        return "redirect:/seckill/orderQuery/" + orderNo;
    }
}