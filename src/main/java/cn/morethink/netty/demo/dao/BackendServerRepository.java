package cn.morethink.netty.demo.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface BackendServerRepository extends BaseMapper<BackendServerInfo> {


}
