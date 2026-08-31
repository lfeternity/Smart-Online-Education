package com.tianji.user.service.impl;

import com.tianji.api.client.trade.TradeClient;
import com.tianji.common.enums.UserType;
import com.tianji.user.domain.dto.StudentFormDTO;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.service.IUserDetailService;
import com.tianji.user.service.IUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.tianji.user.constants.UserConstants.STUDENT_ROLE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StudentServiceImplTest {

    @Test
    void shouldCreateAccountAndStudentDetail() {
        IUserService userService = mock(IUserService.class);
        IUserDetailService detailService = mock(IUserDetailService.class);
        TradeClient tradeClient = mock(TradeClient.class);
        StudentServiceImpl studentService = new StudentServiceImpl(userService, detailService, tradeClient);
        StudentFormDTO form = new StudentFormDTO();
        form.setCellPhone("13898675601");
        form.setPassword("123321");
        form.setCode("123456");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return null;
        }).when(userService).addUserByPhone(any(User.class), eq("123456"));

        studentService.saveStudent(form);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).addUserByPhone(userCaptor.capture(), eq("123456"));
        assertEquals("13898675601", userCaptor.getValue().getCellPhone());
        assertEquals("123321", userCaptor.getValue().getPassword());
        assertEquals(UserType.STUDENT, userCaptor.getValue().getType());

        ArgumentCaptor<UserDetail> detailCaptor = ArgumentCaptor.forClass(UserDetail.class);
        verify(detailService).save(detailCaptor.capture());
        assertEquals(99L, detailCaptor.getValue().getId());
        assertEquals(STUDENT_ROLE_ID, detailCaptor.getValue().getRoleId());
    }
}
