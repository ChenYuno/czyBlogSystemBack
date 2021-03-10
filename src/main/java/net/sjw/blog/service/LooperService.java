package net.sjw.blog.service;

import net.sjw.blog.entity.Looper;
import com.baomidou.mybatisplus.extension.service.IService;
import net.sjw.blog.utils.R;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
public interface LooperService extends IService<Looper> {

    R addLoop(Looper looper);

    R getLoop(String loopId);

    R listLoops();

    R updateLoop(String loopId, Looper looper);

    R deleteLoop(String loopId);
}
