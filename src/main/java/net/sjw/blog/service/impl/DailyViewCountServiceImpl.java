package net.sjw.blog.service.impl;

import net.sjw.blog.entity.DailyViewCount;
import net.sjw.blog.mapper.DailyViewCountMapper;
import net.sjw.blog.service.DailyViewCountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Service
public class DailyViewCountServiceImpl extends ServiceImpl<DailyViewCountMapper, DailyViewCount> implements DailyViewCountService {

}
