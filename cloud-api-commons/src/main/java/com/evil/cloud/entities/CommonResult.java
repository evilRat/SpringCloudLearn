package com.evil.cloud.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult<T> {

    private Integer code;
    private String message;

    private T data;

    //提供给没有数据的返回结果的构造函数
    public CommonResult(Integer code, String message) {
        this(code, message, null);
    }


}
