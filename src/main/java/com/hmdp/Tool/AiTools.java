package com.hmdp.Tool;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiTools {
        @Autowired
        private IShopService shopService;

        @Tool("根据类型分页查询店铺列表")
    public List<Shop> queryShopByType(
            @P("类型id，1美食，2KTV，3美发，4健身，5按摩足疗" +
                    "6美容SPA，7亲子游乐，8酒吧，9轰趴馆，10美甲美睫") Integer typeId,
            @P("分页参数，默认为1") Integer current
        ){
            Page<Shop> page = shopService.query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return page.getRecords();
        }
}
