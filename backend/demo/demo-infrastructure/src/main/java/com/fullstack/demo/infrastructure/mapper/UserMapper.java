package com.fullstack.demo.infrastructure.mapper;

import com.fullstack.common.mybatisplus.mapper.BaseMapperPlus;
import com.fullstack.demo.domain.user.User;

/**
 * 用户 Mapper
 * <p>
 * DemoApplication 的 @MapperScan 只扫描本包，必须落位于此。
 */
public interface UserMapper extends BaseMapperPlus<User> {
}
