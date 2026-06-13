package com.lvcha.web3.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lvcha.web3.pojo.Web3Dict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface Web3DictMapper extends BaseMapper<Web3Dict> {
    @Select("select * from web3_dict where dict_type=#{dictType} and dict_name=#{dictName}")
    Web3Dict getDictByTypeAndName(String dictType,String dictName);

    @Update("update web3_dict set dict_value=#{dictValue} where dict_type=#{dictType} and dict_name=#{dictName}")
    int updateDictValueByTypeAndName(String dictType,String dictName,String dictValue);

}
