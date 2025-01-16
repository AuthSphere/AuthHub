package com.authhub.implementation.authentication;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authhub.domain.implementation.authentication.DefaultMember;
import com.authhub.domain.implementation.authorization.DefaultGroup;
import com.authhub.domain.implementation.authorization.DefaultRole;
import com.authhub.domain.interfaces.authentication.Member;
import com.authhub.domain.interfaces.authorization.Group;
import com.authhub.domain.interfaces.authorization.Role;
import com.authhub.domain.repository.DefaultGroupRepository;
import com.authhub.domain.repository.DefaultMemberRepository;
import com.authhub.domain.repository.DefaultRoleRepository;
import com.authhub.implementation.exception.commonexception.GroupAlreadyAssignedException;
import com.authhub.implementation.exception.commonexception.GroupNotFoundException;
import com.authhub.implementation.exception.commonexception.MemberNotFoundException;
import com.authhub.implementation.exception.commonexception.RoleAlreadyAssignedException;
import com.authhub.implementation.exception.commonexception.RoleNotFoundException;
import com.authhub.implementation.exception.commonexception.UnsupportedGroupTypeException;
import com.authhub.implementation.exception.commonexception.UnsupportedMemberTypeException;
import com.authhub.implementation.exception.commonexception.UnsupportedRoleTypeException;
import com.authhub.implementation.exception.domain.ExceptionType;
import com.authhub.interfaces.authentication.MemberManagementService;

@Service
@Transactional(readOnly = true)
public class DefaultMemberManagementService implements MemberManagementService {
    private final DefaultMemberRepository memberRepository;
    private final DefaultRoleRepository roleRepository;
    private final DefaultGroupRepository groupRepository;

    public DefaultMemberManagementService(
        DefaultMemberRepository memberRepository, 
        DefaultRoleRepository roleRepository, 
        DefaultGroupRepository groupRepository
    ) {
        this.memberRepository = memberRepository;
        this.roleRepository = roleRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional
    public Member createMember(Member member) {
        if (!(member instanceof DefaultMember)) {
            throw new UnsupportedMemberTypeException(ExceptionType.UNSUPPORTED_MEMBER_TYPE.getMessage());
        }
        DefaultMember defaultMember = (DefaultMember) member;
        DefaultMember savedMember = memberRepository.save(defaultMember);
        return savedMember;
    }

    @Override
    @Transactional
    public Member updateMember(Member member) {
        if (!(member instanceof DefaultMember)) {
            throw new UnsupportedMemberTypeException(ExceptionType.UNSUPPORTED_MEMBER_TYPE.getMessage());
        }

        DefaultMember defaultMember = (DefaultMember) member;
        // DB에 이미 있는지 확인
        DefaultMember existingMember = memberRepository.findByUsername(defaultMember.getUsername())
                .orElseThrow(() -> new MemberNotFoundException(ExceptionType.MEMBER_NOT_FOUND.getMessage()));
        
        existingMember.updateFrom(defaultMember);
        DefaultMember updatedMember = memberRepository.save(existingMember);
        return updatedMember;
    }

    @Override
    @Transactional
    public void deleteMember(String username) {
        Optional<DefaultMember> memberOpt = memberRepository.findByUsername(username);
        if (memberOpt.isEmpty()) {
            throw new MemberNotFoundException(ExceptionType.MEMBER_NOT_FOUND.getMessage());
        }
        memberRepository.delete(memberOpt.get());
    }

    @Override
    public Optional<Member> findMemberByUsername(String username) {
        return memberRepository.findByUsername(username).map(member -> (Member) member);
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        if (!(role instanceof DefaultRole)) {
            throw new UnsupportedRoleTypeException(ExceptionType.UNSUPPORTED_ROLE_TYPE.getMessage());
        }
        DefaultRole defaultRole = (DefaultRole) role;
        DefaultRole savedRole = roleRepository.save(defaultRole);
        return savedRole;
    }

    @Override
    @Transactional
    public void assignRoleToMember(String username, String roleName) {
        DefaultMember member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new MemberNotFoundException(ExceptionType.MEMBER_NOT_FOUND.getMessage()));
        DefaultRole role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(ExceptionType.ROLE_NOT_FOUND.getMessage()));

        // 이미 할당된 상태인지 중복 체크 로직 작성
        if (member.hasRole(roleName)) {
            throw new RoleAlreadyAssignedException(ExceptionType.ROLE_ALREADY_ASSIGNED.getMessage());
        }

        member.addToRole(role);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public Group createGroup(Group group) {
        if (!(group instanceof DefaultGroup)) {
            throw new UnsupportedGroupTypeException(ExceptionType.UNSUPPORTED_GROUP_TYPE.getMessage());
        }
        DefaultGroup defaultGroup = (DefaultGroup) group;
        DefaultGroup savedGroup = groupRepository.save(defaultGroup);
        return savedGroup;
    }

    @Override
    @Transactional
    public void addMemberToGroup(String username, String groupName) {
        DefaultMember member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new MemberNotFoundException(ExceptionType.MEMBER_NOT_FOUND.getMessage()));

        DefaultGroup group = groupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new GroupNotFoundException(ExceptionType.GROUP_NOT_FOUND.getMessage()));

        //이미 속해 있는지 중복 체크 로직
        if (member.hasGroup(groupName)) {
            throw new GroupAlreadyAssignedException(ExceptionType.GROUP_ALREADY_ASSIGNED.getMessage());
        }

        member.addToGroup(group);
        memberRepository.save(member);
    }

    @Override
    @Transactional
    public void addRoleToGroup(String roleName, String groupName) {
        DefaultRole role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RoleNotFoundException(ExceptionType.ROLE_NOT_FOUND.getMessage()));
        DefaultGroup group = groupRepository.findByGroupName(groupName)
                .orElseThrow(() -> new GroupNotFoundException(ExceptionType.GROUP_NOT_FOUND.getMessage()));

        //이미 속해 있는지 중복 체크 로직
        if (group.hasRole(roleName)) {
            throw new RoleAlreadyAssignedException(ExceptionType.ROLE_ALREADY_ASSIGNED.getMessage());
        }

        group.addToRole(role);
        groupRepository.save(group);
    }
}
