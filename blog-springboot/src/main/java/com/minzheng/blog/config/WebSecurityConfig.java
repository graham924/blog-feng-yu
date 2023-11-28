package com.minzheng.blog.config;

import com.minzheng.blog.handler.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.session.HttpSessionEventPublisher;


/**
 * Security配置类
 *
 * @author yezhiqiu
 * @date 2021/07/29
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AuthenticationEntryPointImpl authenticationEntryPoint;  // 用户未登录异常处理

    @Autowired
    private AccessDeniedHandlerImpl accessDeniedHandler;    // 用户权限不足的异常处理

    @Autowired
    private AuthenticationSuccessHandlerImpl authenticationSuccessHandler;  // 登录成功处理

    @Autowired
    private AuthenticationFailHandlerImpl authenticationFailHandler;    // 登录失败异常处理

    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;  // 登出成功处理

    // 自定义接口拦截规则
    @Bean
    public FilterInvocationSecurityMetadataSource securityMetadataSource() {
        return new FilterInvocationSecurityMetadataSourceImpl();
    }

    // 自定义访问决策管理器
    @Bean
    public AccessDecisionManager accessDecisionManager() {
        return new AccessDecisionManagerImpl();
    }

    /**
     * session注册方法
     * @return
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    // 配置HttpSessionEventPublisher防用户重复登录
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    /**
     * 密码加密方式 BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置权限
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 配置登录注销路径
        http.formLogin()
                // 登录配置
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailHandler)
                .and()
                // 登出配置
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler);

        // 配置路由权限信息
        http.authorizeRequests()
                // 当有个对象实例实现了FilterSecurityInterceptor，进行对象postProcess增强的时候就会修改其属性
                .withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
                    @Override
                    public <O extends FilterSecurityInterceptor> O postProcess(O fsi) {
                        fsi.setSecurityMetadataSource(securityMetadataSource());    // 设置安全元数据源
                        fsi.setAccessDecisionManager(accessDecisionManager());      // 设置访问决策管理器
                        return fsi;
                    }
                })
                // 首先放行所有的请求，后面再进行筛选
                .anyRequest().permitAll()
                .and()
                // 关闭跨站请求防护
                .csrf().disable().exceptionHandling()
                // 未登录处理
                .authenticationEntryPoint(authenticationEntryPoint)
                // 权限不足处理
                .accessDeniedHandler(accessDeniedHandler)
                .and()
                // 开启session
                .sessionManagement()
                // 指定每个用户的最大并发会话数量，-1 表示不限数量
                .maximumSessions(20)
                // 设置所要使用的 sessionRegistry
                .sessionRegistry(sessionRegistry());

        // gesang+：允许跨域
//        http.cors();

    }

}
